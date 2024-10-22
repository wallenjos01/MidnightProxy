package org.wallentines.mdproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.lang.LangManager;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdproxy.command.CommandExecutor;
import org.wallentines.mdproxy.command.ListCommand;
import org.wallentines.mdproxy.command.ReloadCommand;
import org.wallentines.mdproxy.command.StopCommand;
import org.wallentines.mdproxy.jwt.UsedTokenCache;
import org.wallentines.mdproxy.netty.ConnectionManager;
import org.wallentines.mdproxy.plugin.PluginLoader;
import org.wallentines.mdproxy.plugin.PluginManager;
import org.wallentines.mdproxy.util.CryptUtil;
import org.wallentines.midnightlib.event.HandlerList;
import org.wallentines.midnightlib.registry.Registry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class ProxyServer implements Proxy {

    private static final Logger LOGGER = LoggerFactory.getLogger("ProxyServer");

    private final KeyPair keyPair;
    private final KeyPair reconnectKeyPair;
    //private final Authenticator authenticator;
    private final FileWrapper<ConfigObject> config;
    private final Registry<String, CommandExecutor> commands;
    private final List<StatusEntry> statusEntries = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private final List<AuthRoute> authRoutes = new ArrayList<>();
    private final Map<String, Authenticator> authenticators = new HashMap<>();
    private final ConnectionManager listener;
    private final ConsoleHandler console;
    private final LangManager langManager;
    private final PluginLoader pluginLoader;
    private final UsedTokenCache reconnectTokenCache;
    private final IconCacheImpl iconCache;
    private final int port;
    private final int clientTimeout;
    private final PlayerListImpl playerList;
    private PlayerCountProvider playerCount;

    private final ThreadPoolExecutor authExecutor;

    private int reconnectTimeout;
    private int backendTimeout;
    private int playerLimit;
    private boolean useAuthentication;
    private boolean requireAuth;
    private boolean haproxy;
    private boolean preventProxy;
    private boolean logStatus;
    private boolean legacyPing;
    private Registry<String, Backend> backends = Registry.createStringRegistry();

    // Events
    private final HandlerList<ClientConnection> connected = new HandlerList<>();
    private final HandlerList<ClientConnection> disconnected = new HandlerList<>();
    private final HandlerList<ClientConnection> joined = new HandlerList<>();


    public ProxyServer(FileWrapper<ConfigObject> config, LangManager langManager, PluginLoader pluginLoader) {

        this.config = config;
        this.langManager = langManager;
        this.pluginLoader = pluginLoader;
        this.reconnectTokenCache = new UsedTokenCache("rcid");

        this.playerList = new PlayerListImpl();
        this.playerCount = playerList;

        Path iconCacheDir = Paths.get(getConfig().getString("icon_cache_dir"));
        this.iconCache = new IconCacheImpl(iconCacheDir, getConfig().getInt("icon_cache_size"));

        try { Files.createDirectories(iconCacheDir); } catch (IOException e) {
            throw new RuntimeException("Could not create icon cache directory", e);
        }

        this.port = getConfig().getInt("port");
        this.clientTimeout = getConfig().getInt("client_timeout_ms");

        this.keyPair = CryptUtil.generateKeyPair();
        this.reconnectKeyPair = CryptUtil.generateKeyPair();
        this.commands = Registry.createStringRegistry();

        this.commands.register("stop", new StopCommand());
        this.commands.register("reload", new ReloadCommand());
        this.commands.register("list", new ListCommand());

        this.listener = new ConnectionManager(this);
        this.console = new ConsoleHandler(this);
        this.authExecutor = new ThreadPoolExecutor(1, getConfig().getInt("auth_threads"), 5000, TimeUnit.MILLISECONDS, new SynchronousQueue<>());

        this.pluginLoader.loadAll(this);

        reload();

    }

    public void start() {
        console.start();
        this.listener.startListener();
    }

    @Override
    public void shutdown() {

        if(listener.getBossGroup().isTerminated()) {
            return;
        }

        LOGGER.info("Shutting down...");
        console.stop();
        listener.stop();

    }

    @Override
    public ConfigSection getConfig() {
        ConfigObject out = config.getRoot();
        if(out == null || !out.isSection()) {
            return new ConfigSection();
        }
        return out.asSection();
    }

    @Override
    public void reload() {
        config.load();
        langManager.reload();

        ConfigSection conf = getConfig();

        this.useAuthentication = conf.getBoolean("use_authentication");
        this.requireAuth = conf.getBoolean("force_authentication");
        this.backendTimeout = conf.getInt("backend_timeout_ms");
        this.playerLimit = conf.getInt("player_limit");
        this.reconnectTimeout = conf.getInt("reconnect_timeout_sec");
        this.haproxy = conf.getBoolean("haproxy_protocol");
        this.preventProxy = conf.getBoolean("prevent_proxy_connections");
        this.logStatus = conf.getBoolean("log_status_messages");
        this.legacyPing = conf.getBoolean("reply_to_legacy_ping");

        Registry<String, Backend> backends = Registry.createStringRegistry();

        Backend.SERIALIZER.filteredMapOf(
                (key, err) -> LOGGER.warn("Could not deserialize a Backend with id {}! {}", key, err)
        ).deserialize(ConfigContext.INSTANCE, getConfig().getSection("backends"))
                .getOrThrow()
                .forEach(backends::register);

        this.backends = backends.freeze();

        this.statusEntries.clear();
        this.statusEntries.addAll(getConfig().getListFiltered("status", StatusEntry.SERIALIZER, LOGGER::warn));

        this.routes.clear();
        this.routes.addAll(getConfig().getListFiltered("routes", Route.SERIALIZER, LOGGER::warn));

        this.authRoutes.clear();
        this.authRoutes.addAll(getConfig().getListFiltered("auth_routes", AuthRoute.SERIALIZER.forContext(this), LOGGER::warn));
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public Registry<String, Backend> getBackends() {
        return backends;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return statusEntries;
    }

    @Override
    public List<Route> getRoutes() {
        return routes;
    }

    @Override
    public IconCache getIconCache() {
        return iconCache;
    }

    @Override
    public boolean usesAuthentication() {
        return useAuthentication;
    }

    @Override
    public boolean requiresAuth() {
        return requireAuth;
    }

    @Override
    public Registry<String, CommandExecutor> getCommands() {
        return commands;
    }

    @Override
    public Stream<UUID> getClientIds() {
        return playerList.getPlayerIds();
    }

    @Override
    public ClientConnection getConnection(UUID uuid) {
        return playerList.getPlayer(uuid);
    }

    @Override
    public PlayerList getPlayerList() {
        return playerList;
    }

    @Override
    public int getOnlinePlayers() {
        return playerCount.getOnlinePlayers(this);
    }

    @Override
    public int getPlayerLimit() {
        return playerLimit;
    }

    @Override
    public boolean bypassesPlayerLimit(PlayerInfo info) {
        return false;
    }

    @Override
    public void setPlayerCountProvider(PlayerCountProvider provider) {
        this.playerCount = provider;
    }

    @Override
    public Authenticator getAuthenticator(String type) {
        return authenticators.computeIfAbsent(type, k -> {
            Authenticator.Type t = Authenticator.REGISTRY.get(k);
            if(t == null) return null;
            return t.create(this);
        });
    }

    public List<AuthRoute> getAuthRoutes() {
        return authRoutes;
    }

    public Executor getAuthExecutor() {
        return authExecutor;
    }

    @Override
    public PluginManager getPluginManager() {
        return pluginLoader;
    }

    @Override
    public HandlerList<ClientConnection> clientConnectEvent() {
        return connected;
    }

    @Override
    public HandlerList<ClientConnection> clientDisconnectEvent() {
        return disconnected;
    }

    @Override
    public HandlerList<ClientConnection> clientJoinBackendEvent() {
        return joined;
    }

    public int getReconnectTimeout() {
        return reconnectTimeout;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public KeyPair getReconnectKeyPair() {
        return reconnectKeyPair;
    }

    public UsedTokenCache getTokenCache() {
        return reconnectTokenCache;
    }

    public int getClientTimeout() {
        return clientTimeout;
    }

    public int getBackendTimeout() {
        return backendTimeout;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public boolean useHAProxyProtocol() {
        return haproxy;
    }

    public boolean preventProxyConnections() {
        return preventProxy;
    }

    public boolean logStatusMessages() {
        return logStatus;
    }

    public boolean replyToLegacyPing() {
        return legacyPing;
    }

    public ConnectionManager getConnectionManager() {
        return listener;
    }

}

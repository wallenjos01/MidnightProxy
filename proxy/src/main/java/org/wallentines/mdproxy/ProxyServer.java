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
import org.wallentines.midnightlib.registry.RegistryBase;
import org.wallentines.midnightlib.registry.StringRegistry;

import java.io.File;
import java.security.KeyPair;
import java.util.*;
import java.util.stream.Stream;

public class ProxyServer implements Proxy {

    private static final Logger LOGGER = LoggerFactory.getLogger("ProxyServer");

    private final KeyPair keyPair;
    private final KeyPair reconnectKeyPair;
    private final Authenticator authenticator;
    private final FileWrapper<ConfigObject> config;
    private final StringRegistry<CommandExecutor> commands;
    private final List<StatusEntry> statusEntries = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    //private final HashMap<UUID, ClientConnectionImpl> clients = new HashMap<>();
    private final ConnectionManager listener;
    private final ConsoleHandler console;
    private final LangManager langManager;
    private final PluginLoader pluginLoader;
    private final UsedTokenCache reconnectTokenCache;
    private final IconCacheImpl iconCache;
    private final int port;
    private final int clientTimeout;

    private int reconnectTimeout;
    private int backendTimeout;
    private int playerLimit;
    private boolean onlineMode;
    private boolean requireAuth;
    private boolean haproxy;
    private boolean preventProxy;
    private RegistryBase<String, Backend> backends = new StringRegistry<>();


    // Events
    private final HandlerList<ClientConnection> connected = new HandlerList<>();
    private final HandlerList<ClientConnection> disconnected = new HandlerList<>();
    private final HandlerList<ClientConnection> joined = new HandlerList<>();


    public ProxyServer(FileWrapper<ConfigObject> config, LangManager langManager, PluginLoader pluginLoader) {

        this.config = config;
        this.langManager = langManager;
        this.pluginLoader = pluginLoader;
        this.reconnectTokenCache = new UsedTokenCache("rcid");

        File iconCacheDir = new File(getConfig().getString("icon_cache_dir"));
        this.iconCache = new IconCacheImpl(iconCacheDir, getConfig().getInt("icon_cache_size"));

        if(!iconCacheDir.exists() && !iconCacheDir.mkdirs()) {
            LOGGER.warn("Unable to create icon cache directory!");
        }


        this.port = getConfig().getInt("port");
        this.clientTimeout = getConfig().getInt("client_timeout_ms");

        this.keyPair = CryptUtil.generateKeyPair();
        this.reconnectKeyPair = CryptUtil.generateKeyPair();
        this.authenticator = new Authenticator(this, getConfig().getInt("auth_threads"));
        this.commands = new StringRegistry<>();

        this.commands.register("stop", new StopCommand());
        this.commands.register("reload", new ReloadCommand());
        this.commands.register("list", new ListCommand());

        this.listener = new ConnectionManager(this);
        this.console = new ConsoleHandler(this);

        this.pluginLoader.loadAll(this);

        reload();

    }

    public void start() {

        this.listener.startup();
        console.start();

    }

    @Override
    public void shutdown() {

        LOGGER.info("Shutting down...");

        console.stop();

        listener.shutdown();

        authenticator.close();

    }

    @Override
    public ConfigSection getConfig() {
        return config.getRoot().asSection();
    }

    @Override
    public void reload() {
        config.load();
        langManager.reload();

        this.onlineMode = getConfig().getBoolean("online_mode");
        this.requireAuth = getConfig().getBoolean("force_authentication");
        this.backendTimeout = getConfig().getInt("backend_timeout_ms");
        this.playerLimit = getConfig().getInt("player_limit");
        this.reconnectTimeout = getConfig().getInt("reconnect_timeout_sec");
        this.haproxy = getConfig().getBoolean("haproxy_protocol");
        this.preventProxy = getConfig().getBoolean("prevent_proxy_connections");

        StringRegistry<Backend> backends = new StringRegistry<>();

        Backend.SERIALIZER.filteredMapOf(
                (key, err) -> LOGGER.warn("Could not deserialize a Backend with id " + key + "! " + err)
        ).deserialize(ConfigContext.INSTANCE, getConfig().getSection("backends"))
                .getOrThrow()
                .forEach(backends::register);

        this.backends = backends.freeze();

        this.statusEntries.clear();
        this.statusEntries.addAll(getConfig().getListFiltered("status", StatusEntry.SERIALIZER, LOGGER::warn));

        this.routes.clear();
        this.routes.addAll(getConfig().getListFiltered("routes", Route.SERIALIZER, LOGGER::warn));
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public RegistryBase<String, Backend> getBackends() {
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
    public boolean isOnlineMode() {
        return onlineMode;
    }

    @Override
    public boolean requiresAuth() {
        return requireAuth;
    }

    @Override
    public StringRegistry<CommandExecutor> getCommands() {
        return commands;
    }

    @Override
    public Stream<UUID> getClientIds() {

        return listener.getClientIds();
    }

    @Override
    public ClientConnection getConnection(UUID uuid) {
        return listener.getConnection(uuid);
    }

    @Override
    public int getOnlinePlayers() {
        return listener.getClientCount();
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

//    public void addPlayer(ClientConnectionImpl conn) {
//
//        if(!conn.playerInfoAvailable()) {
//            throw new IllegalStateException("Attempt to add player before connection!");
//        }
//
//        ClientConnectionImpl old = clients.put(conn.uuid(), conn);
//        if(old != null) {
//            old.disconnect();
//        }
//    }
//
//    public void removePlayer(UUID uuid) {
//        ClientConnectionImpl old = clients.remove(uuid);
//        if(old != null) {
//            old.disconnect();
//        }
//    }

    public int getReconnectTimeout() {
        return reconnectTimeout;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public KeyPair getReconnectKeyPair() {
        return reconnectKeyPair;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
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
}

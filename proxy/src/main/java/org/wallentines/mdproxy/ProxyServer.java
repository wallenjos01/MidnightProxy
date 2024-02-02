package org.wallentines.mdproxy;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.lang.LangManager;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdproxy.command.CommandExecutor;
import org.wallentines.mdproxy.command.ReloadCommand;
import org.wallentines.mdproxy.command.StopCommand;
import org.wallentines.mdproxy.netty.ConnectionManager;
import org.wallentines.mdproxy.plugin.PluginLoader;
import org.wallentines.mdproxy.util.CryptUtil;
import org.wallentines.midnightlib.registry.RegistryBase;
import org.wallentines.midnightlib.registry.StringRegistry;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ProxyServer implements Proxy {

    private static final Logger LOGGER = LoggerFactory.getLogger("ProxyServer");

    private final KeyPair keyPair;
    private final Authenticator authenticator;
    private final FileWrapper<ConfigObject> config;
    private final StringRegistry<CommandExecutor> commands;
    private final List<StatusEntry> statusEntries = new ArrayList<>();
    private final HashMap<UUID, ClientConnectionImpl> connected = new HashMap<>();
    private final ConnectionManager listener;
    private final ConsoleHandler console;
    private final LangManager langManager;
    private final PluginLoader pluginLoader;

    private final int port;
    private final int clientTimeout;
    private int reconnectTimeout;
    private int backendTimeout;
    private int playerLimit;
    private boolean requireAuth;
    private RegistryBase<String, Backend> backends = new StringRegistry<>();

    public ProxyServer(FileWrapper<ConfigObject> config, LangManager langManager, PluginLoader pluginLoader) {

        this.config = config;
        this.langManager = langManager;
        this.pluginLoader = pluginLoader;
        reload();

        this.port = getConfig().getInt("port");
        this.clientTimeout = getConfig().getInt("client_timeout");

        this.keyPair = CryptUtil.generateKeyPair();
        this.authenticator = new Authenticator(new YggdrasilAuthenticationService(java.net.Proxy.NO_PROXY).createMinecraftSessionService(), getConfig().getInt("auth_threads"));
        this.reconnectTimeout = getConfig().getInt("reconnect_timeout");
        this.commands = new StringRegistry<>();

        this.commands.register("stop", new StopCommand());
        this.commands.register("reload", new ReloadCommand());

        this.listener = new ConnectionManager(this);
        this.console = new ConsoleHandler(this);
    }

    public void start() {

        this.pluginLoader.loadAll(this);

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

        this.requireAuth = getConfig().getBoolean("online_mode");
        this.backendTimeout = getConfig().getInt("backend_timeout");
        this.playerLimit = getConfig().getInt("player_limit");

        StringRegistry<Backend> backends = new StringRegistry<>();

        Backend.SERIALIZER.filteredMapOf(
                (key, err) -> LOGGER.warn("Could not deserialize a Backend with id " + key + "! " + err)
        ).deserialize(ConfigContext.INSTANCE, getConfig().getSection("backends"))
                .getOrThrow()
                .forEach(backends::register);

        this.backends = backends.freeze();

        this.statusEntries.clear();
        this.statusEntries.addAll(getConfig().getListFiltered("status", StatusEntry.SERIALIZER, LOGGER::warn));
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
    public IconCache getIconCache() {
        return null;
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
    public int getOnlinePlayers() {
        return connected.size();
    }

    @Override
    public int getPlayerLimit() {
        return playerLimit;
    }

    @Override
    public boolean bypassesPlayerLimit(PlayerInfo info) {
        return false;
    }

    public void addPlayer(ClientConnectionImpl conn) {

        if(!conn.playerInfoAvailable()) {
            throw new IllegalStateException("Attempt to add player before connection!");
        }

        ClientConnectionImpl old = connected.put(conn.uuid(), conn);
        if(old != null) {
            old.disconnect();
        }
    }

    public void removePlayer(UUID uuid) {
        ClientConnectionImpl old = connected.remove(uuid);
        if(old != null) {
            old.disconnect();
        }
    }

    public int getReconnectTimeout() {
        return reconnectTimeout;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
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
}

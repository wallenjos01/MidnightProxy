package org.wallentines.mdproxy;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.lang.LangManager;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdproxy.command.CommandExecutor;
import org.wallentines.mdproxy.command.ReloadCommand;
import org.wallentines.mdproxy.command.StopCommand;
import org.wallentines.mdproxy.netty.ConnectionManager;
import org.wallentines.mdproxy.util.CryptUtil;
import org.wallentines.midnightlib.registry.StringRegistry;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class ProxyServer implements Proxy {

    private static final Logger LOGGER = LoggerFactory.getLogger("ProxyServer");

    private final KeyPair keyPair;
    private final Authenticator authenticator;
    private final FileWrapper<ConfigObject> config;
    private final ReconnectCache reconnectCache;
    private final StringRegistry<CommandExecutor> commands;
    private final List<Backend> backends = new ArrayList<>();
    private final ConnectionManager listener;
    private final ConsoleHandler console;
    private final LangManager manager;

    private final int port;
    private final int clientTimeout;
    private int backendTimeout;
    private int playerLimit;
    private boolean requireAuth;

    public ProxyServer(FileWrapper<ConfigObject> config, LangManager manager) {

        this.config = config;
        this.manager = manager;
        reload();

        this.port = getConfig().getInt("port");
        this.clientTimeout = getConfig().getInt("client_timeout");

        this.keyPair = CryptUtil.generateKeyPair();
        this.authenticator = new Authenticator(new YggdrasilAuthenticationService(java.net.Proxy.NO_PROXY).createMinecraftSessionService(), getConfig().getInt("auth_threads"));
        this.reconnectCache = new ReconnectCache(getConfig().getInt("reconnect_threads"), getConfig().getInt("reconnect_timeout"));
        this.commands = new StringRegistry<>();

        this.commands.register("stop", new StopCommand());
        this.commands.register("reload", new ReloadCommand());

        this.listener = new ConnectionManager(this);
        this.console = new ConsoleHandler(this);
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
        reconnectCache.close();

    }

    @Override
    public ConfigSection getConfig() {
        return config.getRoot().asSection();
    }

    @Override
    public void reload() {
        config.load();
        manager.reload();

        this.requireAuth = getConfig().getBoolean("online_mode");
        this.backendTimeout = getConfig().getInt("backend_timeout");
        this.playerLimit = getConfig().getInt("player_limit");

        this.backends.clear();
        this.backends.addAll(getConfig().getListFiltered("backends", Backend.SERIALIZER));
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public List<Backend> getBackends() {
        return backends;
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
        return listener.getConnectedClients();
    }

    @Override
    public int getPlayerLimit() {
        return playerLimit;
    }

    @Override
    public boolean bypassesPlayerLimit(PlayerInfo info) {
        return false;
    }

    public ReconnectCache getReconnectCache() {
        return reconnectCache;
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
        return manager;
    }
}

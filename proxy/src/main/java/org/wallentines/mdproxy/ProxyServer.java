package org.wallentines.mdproxy;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdproxy.netty.ClientChannelInitializer;
import org.wallentines.mdproxy.util.CryptUtil;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class ProxyServer {

    private static final Logger LOGGER = LoggerFactory.getLogger("ProxyServer");

    private final EventLoopGroup eventLoopGroup;
    private final KeyPair keyPair;
    private final Authenticator authenticator;
    private final FileWrapper<ConfigObject> config;
    private final ReconnectCache reconnectCache;
    private final List<Backend> backends = new ArrayList<>();

    private final int port;
    private final int clientTimeout;
    private int backendTimeout;
    private boolean requireAuth;
    private ChannelFuture channel;

    public ProxyServer(FileWrapper<ConfigObject> config) {

        this.eventLoopGroup = new NioEventLoopGroup();

        this.config = config;
        reload();

        this.port = getConfig().getInt("port");
        this.clientTimeout = getConfig().getInt("client_timeout");

        this.keyPair = CryptUtil.generateKeyPair();
        this.authenticator = new Authenticator(new YggdrasilAuthenticationService(Proxy.NO_PROXY).createMinecraftSessionService(), getConfig().getInt("auth_threads"));
        this.reconnectCache = new ReconnectCache(getConfig().getInt("reconnect_threads"), getConfig().getInt("reconnect_timeout"));
    }

    public void startup() throws Exception {

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .channelFactory(NioServerSocketChannel::new)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.IP_TOS, 0x18)
                    .childHandler(new ClientChannelInitializer(this))
                    .group(eventLoopGroup)
                    .localAddress(new InetSocketAddress(port));

            channel = bootstrap.bind().syncUninterruptibly();
            channel.channel().closeFuture().sync();

        } catch (Exception ex) {

            LOGGER.error("An error occurred while the server was running!", ex);

        } finally {
            shutdown();
        }
    }

    public void shutdown() {

        LOGGER.info("Shutting down...");
        channel.channel().close();
    }

    public ConfigSection getConfig() {
        return config.getRoot().asSection();
    }

    public void reload() {
        config.load();

        this.requireAuth = getConfig().getBoolean("online_mode");
        this.backendTimeout = getConfig().getInt("backend_timeout");

        this.backends.clear();
        this.backends.addAll(getConfig().getListFiltered("backends", Backend.SERIALIZER));
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

    public int getPort() {
        return port;
    }

    public int getClientTimeout() {
        return clientTimeout;
    }

    public int getBackendTimeout() {
        return backendTimeout;
    }

    public List<Backend> getBackends() {
        return backends;
    }

    public boolean requiresAuth() {
        return requireAuth;
    }

}

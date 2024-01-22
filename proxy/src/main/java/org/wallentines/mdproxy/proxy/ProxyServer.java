package org.wallentines.mdproxy.proxy;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.flow.FlowControlHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdproxy.Backend;
import org.wallentines.mdproxy.util.CryptUtil;

import java.net.Proxy;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class ProxyServer {

    private static final Logger LOGGER = LoggerFactory.getLogger("ProxyServer");

    private final EventLoopGroup eventLoopGroup;
    private final KeyPair keyPair;
    private final MinecraftSessionService minecraft;
    private final FileWrapper<ConfigObject> config;
    private final int port;

    private boolean requireAuth;
    private final List<Backend> backends = new ArrayList<>();

    private ChannelFuture channel;

    public ProxyServer(FileWrapper<ConfigObject> config) {

        this.eventLoopGroup = new NioEventLoopGroup(1);

        this.config = config;
        reload();

        this.port = getConfig().getInt("port");

        this.keyPair = CryptUtil.generateKeyPair();
        this.minecraft = new YggdrasilAuthenticationService(Proxy.NO_PROXY).createMinecraftSessionService();
    }

    public void startup() throws Exception {


        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {

                            ChannelPipeline p = socketChannel.pipeline();

                            p.addLast("splitter", new PacketSplitter());
                            p.addLast(new FlowControlHandler());
                            p.addLast("decoder", new PacketDecoder());
                            p.addLast("prepender", new LengthPrepender());
                            p.addLast("encoder", new PacketEncoder());
                            p.addLast("handler", new ClientPacketHandler(ProxyServer.this));

                        }
                    });


            channel = bootstrap.group(eventLoopGroup).bind(port).syncUninterruptibly();

        } catch (Exception ex) {

            LOGGER.error("An error occurred while loading the server!");
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

        this.backends.clear();
        this.backends.addAll(getConfig().getListFiltered("backends", Backend.SERIALIZER));
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public MinecraftSessionService getSessionService() {
        return minecraft;
    }

    public int getPort() {
        return port;
    }

    public List<Backend> getBackends() {
        return backends;
    }

    public boolean requiresAuth() {
        return requireAuth;
    }

}

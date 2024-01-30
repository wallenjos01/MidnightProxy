package org.wallentines.mdproxy.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.ClientPacketHandler;
import org.wallentines.mdproxy.ProxyServer;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class ConnectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("ConnectionListener");

    private final EventLoopGroup eventLoopGroup;
    private final ProxyServer server;
    private final Set<ClientPacketHandler> connected;
    private ChannelFuture channel;

    public ConnectionManager(ProxyServer server) {
        this.server = server;
        this.eventLoopGroup = new NioEventLoopGroup();
        this.connected = new HashSet<>();
    }

    public void startup() {

        InetSocketAddress addr = new InetSocketAddress(server.getPort());

        ServerBootstrap bootstrap = new ServerBootstrap()
                .channelFactory(NioServerSocketChannel::new)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.IP_TOS, 0x18)
                .childHandler(new ClientChannelInitializer(this, server))
                .group(eventLoopGroup)
                .localAddress(addr);

        channel = bootstrap.bind().addListener((ChannelFutureListener) future -> {
            if(future.isSuccess()) {
                LOGGER.info("Proxy listening on {}:{}", addr.getHostString(), addr.getPort());
            } else {
                LOGGER.error("An error occurred while starting the server!", future.cause());
                server.shutdown();
            }
        });
    }

    public ChannelFuture getChannel() {
        return channel;
    }

    public void addClientConnection(ClientPacketHandler channel) {
        this.connected.add(channel);
    }

    public void removeClientConnection(ClientPacketHandler channel) {
        this.connected.remove(channel);
    }

    public int getConnectedClients() {
        return connected.size();
    }


    public void shutdown() {

        if (channel != null) {
            channel.channel().close();
        }
        for(ClientPacketHandler c : connected) {
            c.close();
        }
        eventLoopGroup.shutdownGracefully();
    }

}

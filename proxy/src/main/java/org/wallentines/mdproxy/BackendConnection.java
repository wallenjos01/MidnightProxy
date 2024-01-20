package org.wallentines.mdproxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.ServerboundLoginPacket;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class BackendConnection {

    public static final int CONNECTION_TIMEOUT_MS = 5000;

    private final Channel client;
    private final String hostname;
    private final int port;
    private final boolean haproxy;
    private Channel channel;

    public BackendConnection(String host, int port, Channel client, boolean haproxy) {
        this.hostname = host;
        this.port = port;
        this.client = client;
        this.haproxy = haproxy;
    }

    public CompletableFuture<Channel> connect() {

        CompletableFuture<Channel> out = new CompletableFuture<>();
        EventLoopGroup group = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_MS)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new LengthPrepender())
                                .addLast(new PacketEncoder())
                                .addLast(new PacketForwarder(client));

                        if(haproxy) {
                            ch.pipeline().addFirst(HAProxyMessageEncoder.INSTANCE);
                        }
                    }
                });


        this.channel = bootstrap.connect(hostname, port)
                .addListener((ChannelFutureListener) future -> {
                    if(future.isSuccess()) {
                        out.complete(future.channel());
                    } else {
                        out.complete(null);
                    }
                }).channel();

        return out;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isConnected() {
        return channel.isActive();
    }

}

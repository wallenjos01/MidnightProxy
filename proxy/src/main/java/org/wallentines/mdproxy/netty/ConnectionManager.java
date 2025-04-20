package org.wallentines.mdproxy.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.haproxy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.*;
import org.wallentines.mdproxy.packet.PacketRegistry;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ConnectionManager {


    private static final WriteBufferWaterMark WATER_MARK = new WriteBufferWaterMark(1 << 20, 1 << 21);

    private static final Logger LOGGER = LoggerFactory.getLogger("ConnectionListener");

    private final ChannelType channelType;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final EventLoopGroup eventHandlerGroup;
    private final ProxyServer server;
    private final Set<ClientPacketHandler> connected;
    private ChannelFuture listenChannel;

    public ConnectionManager(ProxyServer server, int bossThreads, int workerThreads) {
        this.server = server;

        this.channelType = ChannelType.getBestChannelType();
        this.bossGroup = channelType.createEventLoopGroup("Netty Boss", bossThreads);
        this.workerGroup = channelType.createEventLoopGroup("Netty Worker", workerThreads);
        this.eventHandlerGroup = channelType.createEventLoopGroup("EventHandler", workerThreads);

        this.connected = new HashSet<>();
    }

    public void startListener() {

        InetSocketAddress addr = new InetSocketAddress(server.getPort());

        ServerBootstrap bootstrap = new ServerBootstrap()
                .channelFactory(channelType.serverSocketChannelFactory)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WATER_MARK)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.IP_TOS, 0x20)
                .childHandler(new ClientChannelInitializer(this, server))
                .group(bossGroup, workerGroup)
                .localAddress(addr);

        listenChannel = bootstrap.bind().syncUninterruptibly();
        LOGGER.info("Proxy listening on {}:{}", addr.getHostString(), addr.getPort());
    }

    public CompletableFuture<BackendConnectionImpl> connectToBackend(ClientConnectionImpl conn, Backend backend, int protocolVersion, int timeout) {
        CompletableFuture<BackendConnectionImpl> out = new CompletableFuture<>();

        Bootstrap bootstrap = new Bootstrap()
                .group(conn.getChannel().eventLoop())
                .channelFactory(channelType.socketChannelFactory)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .option(ChannelOption.AUTO_READ, false)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast("frame_enc", new FrameEncoder())
                                .addLast("encoder", new PacketEncoder<>(PacketRegistry.HANDSHAKE));

                        if(backend.haproxy()) {
                            ch.pipeline().addFirst(HAProxyMessageEncoder.INSTANCE);
                        }
                    }
                });


        bootstrap.connect(backend.hostname(), backend.port())
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        Channel channel = future.channel();
                        if(backend.haproxy()) {
                            InetSocketAddress source = conn.socketAddress();
                            InetSocketAddress dest = (InetSocketAddress) channel.remoteAddress();
                            HAProxyProxiedProtocol proto = source.getAddress() instanceof Inet4Address ?
                                    HAProxyProxiedProtocol.TCP4 :
                                    HAProxyProxiedProtocol.TCP6;

                            channel.writeAndFlush(new HAProxyMessage(
                                    HAProxyProtocolVersion.V2,
                                    HAProxyCommand.PROXY,
                                    proto,
                                    source.getAddress().getHostAddress(),
                                    dest.getAddress().getHostAddress(),
                                    source.getPort(),
                                    dest.getPort()
                            ));
                        }
                        out.complete(new BackendConnectionImpl(backend, protocolVersion, channel));

                    } else {
                        out.completeExceptionally(future.cause());
                    }
                });

        return out;
    }

    public ChannelFuture getListenChannel() {
        return listenChannel;
    }

    public void addClientConnection(ClientPacketHandler handler) {
        this.connected.add(handler);
    }

    public int getTotalConnections() {
        return connected.size();
    }

    public void removeClientConnection(ClientPacketHandler handler) {
        ClientConnectionImpl conn = handler.getConnection();
        if(conn != null) server.clientDisconnectEvent().invoke(conn);
        this.connected.remove(handler);
    }

    public Executor getEventExecutor() {
        return eventHandlerGroup;
    }

    public void stop() {

        for(ClientPacketHandler c : connected) {
            c.close();
        }

        if (listenChannel != null) {
            listenChannel.channel().close().syncUninterruptibly();
        }

        eventHandlerGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    public EventLoopGroup getBossGroup() {
        return bossGroup;
    }
}

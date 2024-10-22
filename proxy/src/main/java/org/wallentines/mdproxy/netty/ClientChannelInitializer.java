package org.wallentines.mdproxy.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.BackendConnectionImpl;
import org.wallentines.mdproxy.ClientConnectionImpl;
import org.wallentines.mdproxy.ClientPacketHandler;
import org.wallentines.mdproxy.ProxyServer;
import org.wallentines.mdproxy.packet.PacketRegistry;
import org.wallentines.midnightlib.types.DefaultedSingleton;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class ClientChannelInitializer extends ChannelInitializer<Channel> {

    private static final Logger LOGGER = LoggerFactory.getLogger("ClientChannelInitializer");
    private final ConnectionManager manager;
    private final ProxyServer server;

    public ClientChannelInitializer(ConnectionManager manager, ProxyServer server) {
        this.server = server;
        this.manager = manager;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {

        channel.pipeline()
                .addLast("frame_dec", new FrameDecoder())
                .addLast(new ReadTimeoutHandler(server.getClientTimeout(), TimeUnit.MILLISECONDS))
                .addLast("frame_enc", new FrameEncoder())
                .addLast("decoder", new PacketDecoder<>(PacketRegistry.HANDSHAKE))
                .addLast("encoder", new PacketEncoder<>());

        DefaultedSingleton<InetSocketAddress> addr = new DefaultedSingleton<>(((InetSocketAddress) channel.remoteAddress()));

        if(server.replyToLegacyPing()) {
            channel.pipeline().addFirst("legacy_ping", new LegacyPingHandler(server, addr));
        }

        if(server.useHAProxyProtocol()) {
            channel.pipeline().addFirst("haproxy_decoder", new HAProxyMessageDecoder());
            channel.pipeline().addFirst("haproxy_handler", new HAProxyHandler(addr));
        }

        ClientPacketHandler handler = new ClientPacketHandler(channel, addr, server);
        channel.pipeline().addLast("handler", new PacketHandler<>(handler));

        manager.addClientConnection(handler);
        channel.closeFuture().addListener(future -> {
            ClientConnectionImpl conn = handler.getConnection();
            if(conn != null) {
                BackendConnectionImpl bConn = conn.getBackendConnection();
                if(bConn != null) {
                    bConn.close();
                    LOGGER.info("Client disconnected: {}", handler.getUsername());
                }
                if(conn.profileAvailable()) {
                    server.getPlayerList().removePlayer(conn.uuid());
                }
            }
            manager.removeClientConnection(handler);
        });

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("An exception occurred while initializing a client channel!", cause);
    }
}

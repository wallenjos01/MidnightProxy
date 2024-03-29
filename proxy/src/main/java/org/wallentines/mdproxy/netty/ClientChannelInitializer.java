package org.wallentines.mdproxy.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.wallentines.mdproxy.ClientPacketHandler;
import org.wallentines.mdproxy.ProxyServer;
import org.wallentines.mdproxy.packet.PacketRegistry;
import org.wallentines.midnightlib.types.DefaultedSingleton;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class ClientChannelInitializer extends ChannelInitializer<Channel> {


    private final ConnectionManager manager;
    private final ProxyServer server;

    public ClientChannelInitializer(ConnectionManager manager, ProxyServer server) {
        this.server = server;
        this.manager = manager;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {

        channel.pipeline()
                .addLast(new ReadTimeoutHandler(server.getClientTimeout(), TimeUnit.MILLISECONDS))
                .addLast("frame_enc", new FrameEncoder())
                .addLast("encoder", new PacketEncoder<>())
                .addLast("frame_dec", new FrameDecoder())
                .addLast("decoder", new PacketDecoder<>(PacketRegistry.HANDSHAKE));

        DefaultedSingleton<InetSocketAddress> addr = new DefaultedSingleton<>(((InetSocketAddress) channel.remoteAddress()));

        if(server.useHAProxyProtocol()) {
            channel.pipeline().addFirst("haproxy_decoder", new HAProxyMessageDecoder());
            channel.pipeline().addFirst("haproxy_handler", new HAProxyHandler(addr));
        }

        ClientPacketHandler handler = new ClientPacketHandler(channel, addr, server);
        channel.pipeline().addLast("handler", new PacketHandler<>(handler));

        manager.addClientConnection(handler);
        channel.closeFuture().addListener(future -> {
            manager.removeClientConnection(handler);
        });

    }
}

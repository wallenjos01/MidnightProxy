package org.wallentines.mdproxy.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.wallentines.mdproxy.ClientPacketHandler;
import org.wallentines.mdproxy.ProxyServer;
import org.wallentines.mdproxy.packet.PacketRegistry;

import java.util.concurrent.TimeUnit;

public class ClientChannelInitializer extends ChannelInitializer<Channel> {


    private final ProxyServer server;

    public ClientChannelInitializer(ProxyServer server) {
        this.server = server;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {

        channel.pipeline()
                .addLast(new ReadTimeoutHandler(server.getClientTimeout(), TimeUnit.MILLISECONDS))
                .addLast("frame_enc", new FrameEncoder())
                .addLast("encoder", new PacketEncoder<>())
                .addLast("frame_dec", new FrameDecoder())
                .addLast("decoder", new PacketDecoder<>(PacketRegistry.HANDSHAKE));

        ClientPacketHandler handler = new ClientPacketHandler(channel, server);
        channel.pipeline().addLast("handler", new PacketHandler<>(handler));

    }
}

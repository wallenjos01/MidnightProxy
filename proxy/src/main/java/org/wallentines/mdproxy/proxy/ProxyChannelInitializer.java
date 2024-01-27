package org.wallentines.mdproxy.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.wallentines.mdproxy.packet.PacketRegistry;

public class ProxyChannelInitializer extends ChannelInitializer<Channel> {


    private final ProxyServer server;

    public ProxyChannelInitializer(ProxyServer server) {
        this.server = server;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {

        channel.pipeline()
                .addLast("frame_enc", new FrameEncoder())
                .addLast("encoder", new PacketEncoder())
                .addLast("frame_dec", new FrameDecoder())
                .addLast("decoder", new PacketDecoder(PacketRegistry.HANDSHAKE))
                .addLast("handler", new ClientPacketHandler(channel, server))
                .addLast(new ExceptionHandler())
                .addFirst(new LoggingHandler(LogLevel.INFO));
    }
}

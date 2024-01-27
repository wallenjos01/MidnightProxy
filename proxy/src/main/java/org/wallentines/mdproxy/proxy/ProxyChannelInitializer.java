package org.wallentines.mdproxy.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.flow.FlowControlHandler;
import org.wallentines.mdproxy.packet.PacketRegistry;

public class ProxyChannelInitializer extends ChannelInitializer<Channel> {


    private final ProxyServer server;

    public ProxyChannelInitializer(ProxyServer server) {
        this.server = server;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {

        channel.pipeline()
                .addLast("splitter", new FrameDecoder())
                .addLast(new FlowControlHandler())
                .addLast("decoder", new PacketDecoder(PacketRegistry.HANDSHAKE))
                .addLast("prepender", new FrameEncoder())
                .addLast("encoder", new PacketEncoder())
                .addLast("handler", new ClientPacketHandler(channel, server));
    }
}

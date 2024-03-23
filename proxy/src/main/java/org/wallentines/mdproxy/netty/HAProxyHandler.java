package org.wallentines.mdproxy.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import org.wallentines.midnightlib.types.DefaultedSingleton;

import java.net.InetSocketAddress;

public class HAProxyHandler extends SimpleChannelInboundHandler<HAProxyMessage> {

    private final DefaultedSingleton<InetSocketAddress> address;

    HAProxyHandler(DefaultedSingleton<InetSocketAddress> address) {
        this.address = address;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HAProxyMessage haProxyMessage) throws Exception {

        PacketHandler<?> handler = ctx.pipeline().get(PacketHandler.class);
        if(handler != null) {
            address.set(new InetSocketAddress(haProxyMessage.sourceAddress(), haProxyMessage.sourcePort()));
        }

        ctx.pipeline().remove("haproxy_decoder");
        ctx.pipeline().remove("haproxy_handler");

    }
}

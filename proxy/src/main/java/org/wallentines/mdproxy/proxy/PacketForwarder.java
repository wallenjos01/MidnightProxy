package org.wallentines.mdproxy.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PacketForwarder extends SimpleChannelInboundHandler<ByteBuf> {

    private final Channel forwardTarget;

    public PacketForwarder(Channel forwardTarget) {
        this.forwardTarget = forwardTarget;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf data) throws Exception {

        if(!forwardTarget.isWritable()) {
            ctx.close();
            return;
        }
        forwardTarget.writeAndFlush(data);

    }
}

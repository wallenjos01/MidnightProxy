package org.wallentines.mdproxy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketForwarder extends SimpleChannelInboundHandler<ByteBuf> {


    private static final Logger LOGGER = LoggerFactory.getLogger(PacketForwarder.class);
    private final Channel forwardTarget;

    public PacketForwarder(Channel forwardTarget) {
        this.forwardTarget = forwardTarget;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf data) throws Exception {
        forwardTarget.writeAndFlush(data.retain());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        forwardTarget.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("An exception occurred while forwarding a packet!", cause);
    }
}

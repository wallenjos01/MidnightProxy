package org.wallentines.mdproxy.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.packet.Packet;

public class PacketHandler<T> extends SimpleChannelInboundHandler<Packet<T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger("PacketHandler");

    private final T handler;

    public PacketHandler(T handler) {
        this.handler = handler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet<T> pck) throws Exception {

        try {
            pck.handle(handler);
        } finally {
            ReferenceCountUtil.release(pck);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("An exception occurred while handling a packet!", cause);
        ctx.channel().close();
    }
}

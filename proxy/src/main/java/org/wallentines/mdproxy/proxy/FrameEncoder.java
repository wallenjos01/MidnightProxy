package org.wallentines.mdproxy.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.wallentines.mdproxy.util.PacketBufferUtil;

public class FrameEncoder extends MessageToByteEncoder<ByteBuf> {

    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, ByteBuf out) {

        int length = byteBuf.readableBytes();
        PacketBufferUtil.writeVarInt(out, length);
        out.writeBytes(byteBuf);
    }
}

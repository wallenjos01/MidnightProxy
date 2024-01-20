package org.wallentines.mdproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.List;

public class LengthPrepender extends MessageToMessageEncoder<ByteBuf> {

    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out) {

        int length = byteBuf.readableBytes();

        ByteBuf len = Unpooled.buffer();
        PacketBufferUtil.writeVarInt(len, length);

        out.add(len);
        out.add(byteBuf);
    }
}

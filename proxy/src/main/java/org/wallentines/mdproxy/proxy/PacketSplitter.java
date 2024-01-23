package org.wallentines.mdproxy.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.List;

public class PacketSplitter extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        byteBuf.markReaderIndex();
        byte[] bs = new byte[3];
        for (int i = 0; i < bs.length; ++i) {
            if (!byteBuf.isReadable()) {
                byteBuf.resetReaderIndex();
                return;
            }
            bs[i] = byteBuf.readByte();
            if (bs[i] < 0) continue;
            ByteBuf buf = Unpooled.wrappedBuffer(bs);
            try {
                int j = PacketBufferUtil.readVarInt(buf);
                if (byteBuf.readableBytes() < j) {
                    byteBuf.resetReaderIndex();
                    return;
                }
                list.add(byteBuf.readBytes(j));
                return;
            }
            finally {
                buf.release();
            }
        }
        throw new CorruptedFrameException("length wider than 21-bit");
    }
}

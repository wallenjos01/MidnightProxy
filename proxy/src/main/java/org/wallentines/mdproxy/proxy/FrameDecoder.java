package org.wallentines.mdproxy.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.List;

public class FrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf data, List<Object> out) {



        data.markReaderIndex();
        byte[] bs = new byte[3];
        for (int i = 0; i < bs.length; ++i) {
            if (!data.isReadable()) {
                data.resetReaderIndex();
                return;
            }
            bs[i] = data.readByte();
            if (bs[i] < 0) continue;
            ByteBuf buf = Unpooled.wrappedBuffer(bs);
            try {
                int j = PacketBufferUtil.readVarInt(buf);
                if (data.readableBytes() < j) {
                    data.resetReaderIndex();
                    return;
                }
                out.add(data.readBytes(j));
                return;
            }
            finally {
                buf.release();
            }
        }
        throw new CorruptedFrameException("length wider than 21-bit");
    }
}

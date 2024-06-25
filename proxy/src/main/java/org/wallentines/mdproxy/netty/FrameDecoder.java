package org.wallentines.mdproxy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdproxy.VarInt;

import java.util.List;

public class FrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf data, List<Object> out) {

        if(!data.isReadable()) return;

        SerializeResult<VarInt> vLength = VarInt.readPartial(data, 3);
        if(!vLength.isComplete()) {
            ctx.channel().close();
            return;
        }

        int length = vLength.getOrThrow().value();

        if(length == 0 || data.readableBytes() < length) {
            data.resetReaderIndex();
            return;
        }

        out.add(data.readRetainedSlice(length));
    }
}

package org.wallentines.mdproxy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdproxy.VarInt;

import java.util.List;

public class FrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf data, List<Object> out) {

        if(!ctx.channel().isActive()) {
            data.clear();
            return;
        }
        if(!data.isReadable()) {
            return;
        }

        int length;
        SerializeResult<VarInt> vLength = VarInt.readPartial(data, 3);
        if(!vLength.isComplete() || (length = vLength.getOrThrow().value()) <= 0 || !data.isReadable(length)) {
            data.clear();
            ctx.channel().close();
            return;
        }

        out.add(data.readRetainedSlice(length));
    }
}

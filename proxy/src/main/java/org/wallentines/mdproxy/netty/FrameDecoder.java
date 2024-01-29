package org.wallentines.mdproxy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import org.wallentines.mdproxy.VarInt;

import java.util.List;

public class FrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf data, List<Object> out) {

        if(!data.isReadable()) return;

        VarInt vLength = VarInt.read(data, 3);
        int length = vLength.value();

        if(length == 0) return;
        if(length < 0) {
            data.clear();
            throw new DecoderException("Received packet with negative length!");
        }

        if(data.isReadable(length)) {
            out.add(data.readRetainedSlice(length));
        }
    }
}

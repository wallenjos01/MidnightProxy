package org.wallentines.mdproxy.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import javax.crypto.Cipher;
import java.util.List;

public class CryptDecoder  extends MessageToMessageDecoder<ByteBuf> {

    private final Cipher cipher;

    public CryptDecoder(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf data, List<Object> list) throws Exception {

        int cryptSize = data.readableBytes();
        int plainSize = cipher.getOutputSize(cryptSize);

        ByteBuf out = Unpooled.buffer(plainSize);
        out.writeBytes(cipher.update(data.array(), 0, cryptSize));

        list.add(out);
    }
}

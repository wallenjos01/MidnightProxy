package org.wallentines.mdproxy.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import javax.crypto.Cipher;
import java.util.List;

public class CryptEncoder extends MessageToMessageEncoder<ByteBuf> {

    private final Cipher cipher;

    public CryptEncoder(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf data, List<Object> list) throws Exception {

        int cryptSize = data.readableBytes();
        int plainSize = cipher.getOutputSize(cryptSize);

        ByteBuf out = Unpooled.buffer(plainSize);
        out.writeBytes(cipher.update(data.array(), 0, cryptSize));

        list.add(out);
    }
}

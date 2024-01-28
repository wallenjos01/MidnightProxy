package org.wallentines.mdproxy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CryptDecoder  extends MessageToMessageDecoder<ByteBuf> {

    private static final Logger LOGGER = LoggerFactory.getLogger("CryptDecoder");
    private final WrappedCipher cipher;

    public CryptDecoder(WrappedCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf data, List<Object> list) throws Exception {

//        int cryptSize = data.readableBytes();
//        int plainSize = cipher.getOutputSize(cryptSize);
//
//        byte[] crypted = new byte[cryptSize];
//        data.readBytes(crypted, 0, cryptSize);
//
//        ByteBuf out = ctx.alloc().heapBuffer(plainSize);
//        out.writerIndex(cipher.update(crypted, 0, cryptSize, out.array(), out.arrayOffset()));
//
//        list.add(out);

        try {
            ByteBuf out = ctx.alloc().heapBuffer(cipher.getOutputLength(data.readableBytes()));
            cipher.cipher(data, out);
            list.add(out.retain());
        } catch (Exception ex) {
            data.release();
            throw new DecoderException("An exception occurred while decrypting a packet!", ex);
        }
    }
}

package org.wallentines.mdproxy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptEncoder extends MessageToByteEncoder<ByteBuf> {

    private static final Logger LOGGER = LoggerFactory.getLogger("CryptEncoder");
    private final WrappedCipher cipher;

    public CryptEncoder(WrappedCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf data, ByteBuf out) throws Exception {

        try {
            cipher.cipher(data, out);

        } catch (Exception ex) {
            data.release();
            throw new EncoderException("An exception occurred while encrypting a packet!", ex);
        }
    }
}

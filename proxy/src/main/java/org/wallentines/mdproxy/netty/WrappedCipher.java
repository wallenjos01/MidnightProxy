package org.wallentines.mdproxy.netty;

import io.netty.buffer.ByteBuf;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;

public class WrappedCipher {

    public static WrappedCipher forEncryption(SecretKey key) throws GeneralSecurityException {
        return new WrappedCipher(Cipher.ENCRYPT_MODE, key);
    }

    public static WrappedCipher forDecryption(SecretKey key) throws GeneralSecurityException {
        return new WrappedCipher(Cipher.DECRYPT_MODE, key);
    }


    private final Cipher cipher;

    private WrappedCipher(int mode, SecretKey key) throws GeneralSecurityException  {

        this.cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        this.cipher.init(mode, key, new IvParameterSpec(key.getEncoded()));

    }

    public int getOutputLength(int length) {
        return cipher.getOutputSize(length);
    }

    public void cipher(ByteBuf buffer, ByteBuf out) {

        int inputLength = buffer.readableBytes();

        byte[] input = new byte[inputLength];
        buffer.readBytes(input);

        byte[] output = new byte[cipher.getOutputSize(inputLength)];

        try {

            cipher.update(input, 0, inputLength, output, 0);

        } catch (ShortBufferException ex) {
            throw new IllegalStateException("Not enough room for ciphered data!");
        }

        out.writeBytes(output);
    }

}

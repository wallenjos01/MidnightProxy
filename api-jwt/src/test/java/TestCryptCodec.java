import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.mdproxy.jwt.CryptCodec;

import java.util.Base64;

public class TestCryptCodec {

    @Test
    public void testAES128() {

        CryptCodec<CryptCodec.CompoundKey> codec = CryptCodec.A128CBC_HS256();

        byte[] data = "my secret data".getBytes();

        byte[] encrypted = codec.encrypt(data, new byte[0]).cipherText();
        byte[] decrypted = codec.decrypt(encrypted);

        Assertions.assertArrayEquals(data, decrypted);
    }

    @Test
    public void testAES192() {

        CryptCodec<CryptCodec.CompoundKey> codec = CryptCodec.A192CBC_HS384();


        byte[] data = "my secret data".getBytes();

        byte[] encrypted = codec.encrypt(data, new byte[0]).cipherText();
        byte[] decrypted = codec.decrypt(encrypted);

        Assertions.assertArrayEquals(data, decrypted);
    }

    @Test
    public void testAES256() {

        CryptCodec<CryptCodec.CompoundKey> codec = CryptCodec.A256CBC_HS512();

        byte[] data = "my secret data".getBytes();

        byte[] encrypted = codec.encrypt(data,  new byte[0]).cipherText();
        byte[] decrypted = codec.decrypt(encrypted);

        Assertions.assertArrayEquals(data, decrypted);
    }

    @Test
    public void testB64() {

        Base64.Decoder decoder = Base64.getUrlDecoder();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        CryptCodec<CryptCodec.CompoundKey> codec = CryptCodec.A256CBC_HS512();

        byte[] data = "my secret data".getBytes();
        byte[] encrypted = codec.encrypt(data,new byte[0]).cipherText();

        // Cycle cipherText
        String b64 = encoder.encodeToString(encrypted);
        byte[] parsedB64 = decoder.decode(b64);
        Assertions.assertArrayEquals(encrypted, parsedB64);

        // Cycle IV
        String ivb64 = encoder.encodeToString(codec.getIV());
        byte[] parsedIvB64 = decoder.decode(ivb64);
        Assertions.assertArrayEquals(codec.getIV(), parsedIvB64);

        byte[] decrypted = codec.decrypt(parsedB64);

        Assertions.assertArrayEquals(data, decrypted);

    }
}

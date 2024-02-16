import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.mdproxy.jwt.CryptCodec;

import java.util.Base64;
import java.util.Random;

public class TestCryptCodec {

    @Test
    public void testAES128() {

        Random rand = new Random();
        CryptCodec<CryptCodec.CompoundKey> codec = CryptCodec.A128CBC_HS256(rand);

        byte[] iv = new byte[16];
        rand.nextBytes(iv);

        byte[] data = "my secret data".getBytes();

        byte[] encrypted = codec.encrypt(data, iv, new byte[0]).cipherText();
        byte[] decrypted = codec.decrypt(encrypted, iv);

        Assertions.assertArrayEquals(data, decrypted);
    }

    @Test
    public void testAES192() {

        Random rand = new Random();
        CryptCodec<CryptCodec.CompoundKey> codec = CryptCodec.A192CBC_HS384(rand);

        byte[] iv = new byte[16];
        rand.nextBytes(iv);

        byte[] data = "my secret data".getBytes();

        byte[] encrypted = codec.encrypt(data, iv, new byte[0]).cipherText();
        byte[] decrypted = codec.decrypt(encrypted, iv);

        Assertions.assertArrayEquals(data, decrypted);
    }

    @Test
    public void testAES256() {

        Random rand = new Random();
        CryptCodec<CryptCodec.CompoundKey> codec = CryptCodec.A256CBC_HS512(rand);

        byte[] iv = new byte[16];
        rand.nextBytes(iv);

        byte[] data = "my secret data".getBytes();

        byte[] encrypted = codec.encrypt(data, iv, new byte[0]).cipherText();
        byte[] decrypted = codec.decrypt(encrypted, iv);

        Assertions.assertArrayEquals(data, decrypted);
    }

    @Test
    public void testB64() {

        Base64.Decoder decoder = Base64.getUrlDecoder();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        Random rand = new Random();
        CryptCodec<CryptCodec.CompoundKey> codec = CryptCodec.A256CBC_HS512(rand);

        byte[] iv = new byte[16];
        rand.nextBytes(iv);

        byte[] data = "my secret data".getBytes();
        byte[] encrypted = codec.encrypt(data, iv, new byte[0]).cipherText();


        // Cycle cipherText
        String b64 = encoder.encodeToString(encrypted);
        byte[] parsedB64 = decoder.decode(b64);
        Assertions.assertArrayEquals(encrypted, parsedB64);

        // Cycle IV
        String ivb64 = encoder.encodeToString(iv);
        byte[] parsedIvB64 = decoder.decode(ivb64);
        Assertions.assertArrayEquals(iv, parsedIvB64);


        byte[] decrypted = codec.decrypt(parsedB64, parsedIvB64);

        Assertions.assertArrayEquals(data, decrypted);

    }
}

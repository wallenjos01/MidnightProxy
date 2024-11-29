import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.mdproxy.jwt.KeyCodec;

import javax.crypto.SecretKey;
import java.security.*;
import java.util.Base64;
import java.util.Random;

public class TestKeyCodec {

    @Test
    public void testRSA() throws GeneralSecurityException {

        KeyPair kp = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyCodec<PublicKey, PrivateKey> codec = KeyCodec.RSA_OAEP(kp);

        Random rand = new Random();
        byte[] key = new byte[32];
        rand.nextBytes(key);

        byte[] enciphered = codec.encode(key);
        byte[] deciphered = codec.decode(enciphered);

        Assertions.assertArrayEquals(key, deciphered);
    }

    @Test
    public void testRSAAsync() throws GeneralSecurityException {

        KeyPair kp = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyCodec<PublicKey, PrivateKey> encCodec = KeyCodec.RSA_OAEP(kp.getPublic());
        KeyCodec<PublicKey, PrivateKey> decCodec = KeyCodec.RSA_OAEP(kp.getPrivate());

        Random rand = new Random();
        byte[] key = new byte[32];
        rand.nextBytes(key);

        byte[] enciphered = encCodec.encode(key);
        byte[] deciphered = decCodec.decode(enciphered);

        Assertions.assertArrayEquals(key, deciphered);
    }

    @Test
    public void testAES128() throws GeneralSecurityException {

        Random rand = new Random();
        byte[] key = new byte[16];
        rand.nextBytes(key);

        KeyCodec<SecretKey, SecretKey> codec = KeyCodec.A128KW(key);
        byte[] enciphered = codec.encode(key);
        byte[] deciphered = codec.decode(enciphered);

        Assertions.assertArrayEquals(key, deciphered);
    }

    @Test
    public void testAES192() throws GeneralSecurityException {

        Random rand = new Random();
        byte[] key = new byte[24];
        rand.nextBytes(key);

        KeyCodec<SecretKey, SecretKey> codec = KeyCodec.A192KW(key);
        byte[] enciphered = codec.encode(key);
        byte[] deciphered = codec.decode(enciphered);

        Assertions.assertArrayEquals(key, deciphered);
    }

    @Test
    public void testAES256() throws GeneralSecurityException {

        Random rand = new Random();
        byte[] key = new byte[32];
        rand.nextBytes(key);

        KeyCodec<SecretKey, SecretKey> codec = KeyCodec.A256KW(key);
        byte[] enciphered = codec.encode(key);
        byte[] deciphered = codec.decode(enciphered);

        Assertions.assertArrayEquals(key, deciphered);
    }

    @Test
    public void testB64() throws GeneralSecurityException {

        Base64.Decoder decoder = Base64.getUrlDecoder();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        KeyPair kp = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyCodec<PublicKey, PrivateKey> encCodec = KeyCodec.RSA_OAEP(kp.getPublic());
        KeyCodec<PublicKey, PrivateKey> decCodec = KeyCodec.RSA_OAEP(kp.getPrivate());

        Random rand = new Random();
        byte[] key = new byte[32];
        rand.nextBytes(key);

        byte[] enciphered = encCodec.encode(key);

        String b64 = encoder.encodeToString(enciphered);
        byte[] parsedB64 = decoder.decode(b64);

        Assertions.assertArrayEquals(enciphered, parsedB64);

        byte[] deciphered = decCodec.decode(parsedB64);

        Assertions.assertArrayEquals(key, deciphered);

    }

}

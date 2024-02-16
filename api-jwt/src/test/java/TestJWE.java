import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.mdproxy.jwt.*;

import javax.crypto.SecretKey;
import java.security.*;
import java.time.Instant;
import java.util.Random;

public class TestJWE {


    @Test
    public void testRSA() throws GeneralSecurityException {

        KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        Random rand = new Random();

        KeyCodec<PublicKey, PrivateKey> codec = KeyCodec.RSA_OAEP(pair);
        CryptCodec<CryptCodec.CompoundKey> crypt = CryptCodec.A128CBC_HS256(rand);

        Instant issued = Instant.now();
        JWESerializer.JWE jwe = new JWTBuilder()
                .issuedAt(issued)
                .issuedBy("test")
                .encrypted(codec, crypt);

        String encoded = jwe.asString(codec, rand).getOrThrow();
        JWT decrypted = JWESerializer.read(encoded, KeySupplier.of(codec.getDecryptionKey(), codec.getAlgorithm().getDecryptionKeyType())).getOrThrow();

        Assertions.assertEquals(KeyCodec.Algorithm.REGISTRY.getId(codec.getAlgorithm()), decrypted.header().getString("alg"));
        Assertions.assertEquals(CryptCodec.Algorithm.REGISTRY.getId(crypt.getAlgorithm()), decrypted.header().getString("enc"));
        Assertions.assertEquals("JWT", decrypted.header().getString("typ"));
        Assertions.assertEquals("test", decrypted.getIssuer());
        Assertions.assertEquals(issued.getEpochSecond(), decrypted.getIssuedAt().getEpochSecond());

    }

    @Test
    public void testAES() throws GeneralSecurityException {


        Random rand = new Random();


        byte[] aes = new byte[16];
        rand.nextBytes(aes);

        KeyCodec<SecretKey, SecretKey> codec = KeyCodec.A128KW(aes);
        CryptCodec<CryptCodec.CompoundKey> crypt = CryptCodec.A128CBC_HS256(rand);

        Instant issued = Instant.now();
        JWESerializer.JWE jwe = new JWTBuilder()
                .issuedAt(issued)
                .issuedBy("test")
                .encrypted(codec, crypt);

        String encoded = jwe.asString(codec, rand).getOrThrow();
        JWT decrypted = JWESerializer.read(encoded, KeySupplier.of(codec.getDecryptionKey(), codec.getAlgorithm().getDecryptionKeyType())).getOrThrow();

        Assertions.assertEquals(KeyCodec.Algorithm.REGISTRY.getId(codec.getAlgorithm()), decrypted.header().getString("alg"));
        Assertions.assertEquals(CryptCodec.Algorithm.REGISTRY.getId(crypt.getAlgorithm()), decrypted.header().getString("enc"));
        Assertions.assertEquals("JWT", decrypted.header().getString("typ"));
        Assertions.assertEquals("test", decrypted.getIssuer());
        Assertions.assertEquals(issued.getEpochSecond(), decrypted.getIssuedAt().getEpochSecond());

    }

}

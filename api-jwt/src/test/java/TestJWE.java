import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.mdproxy.jwt.*;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestJWE {

    @Test
    public void testRSA() throws GeneralSecurityException {

        KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        KeyCodec<PublicKey, PrivateKey> codec = KeyCodec.RSA_OAEP(pair);

        testJWE(codec);

    }

    @Test
    public void testAES128() throws GeneralSecurityException {

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);

        SecretKey key = keyGen.generateKey();

        KeyCodec<SecretKey, SecretKey> codec = KeyCodec.A128KW(key);

        testJWE(codec);
    }

    @Test
    public void testAES192() throws GeneralSecurityException {

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(192);

        SecretKey key = keyGen.generateKey();

        KeyCodec<SecretKey, SecretKey> codec = KeyCodec.A192KW(key);
        testJWE(codec);
    }

    @Test
    public void testAES256() throws GeneralSecurityException {

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);

        SecretKey key = keyGen.generateKey();

        KeyCodec<SecretKey, SecretKey> codec = KeyCodec.A256KW(key);

        testJWE(codec);
    }

    private <KE extends Key, KD extends Key> void testJWE(KeyCodec<KE, KD> codec) throws GeneralSecurityException {

        testJWE(codec, CryptCodec.A128CBC_HS256());
        testJWE(codec, CryptCodec.A192CBC_HS384());
        testJWE(codec, CryptCodec.A256CBC_HS512());

    }

    private <KE extends Key, KD extends Key, C> void testJWE(KeyCodec<KE, KD> codec, CryptCodec<C> crypt) throws GeneralSecurityException {

        AtomicInteger completed = new AtomicInteger();
        ThreadPoolExecutor exe = new ThreadPoolExecutor(4, 12, 15000L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(100));

        List<CompletableFuture<?>> futures = new ArrayList<>(50);
        for(int i = 0; i < 50; i++) {
            final int index = i;

            futures.add(CompletableFuture.runAsync(() -> {
                Instant issued = Instant.now();
                JWESerializer.JWE jwe = new JWTBuilder()
                        .issuedAt(issued)
                        .issuedBy("test" + index)
                        .encrypted(codec, crypt);

                String encoded = jwe.asString(codec).getOrThrow();
                JWT decrypted = JWESerializer.read(encoded, KeySupplier.of(codec.getDecryptionKey(), codec.getAlgorithm().getDecryptionKeyType())).getOrThrow();

                Assertions.assertEquals(KeyCodec.ALGORITHMS.getId(codec.getAlgorithm()), decrypted.header().getString("alg"));
                Assertions.assertEquals(CryptCodec.ALGORITHMS.getId(crypt.getAlgorithm()), decrypted.header().getString("enc"));
                Assertions.assertEquals("JWT", decrypted.header().getString("typ"));
                Assertions.assertEquals("test" + index, decrypted.getIssuer());
                Assertions.assertEquals(issued.getEpochSecond(), decrypted.getIssuedAt().getEpochSecond());

            }, exe).whenComplete((v, ex) -> {
                if(ex != null) {
                    Assertions.fail("Test " + index + " failed!", ex);
                }
                completed.getAndIncrement();
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).orTimeout(15000L, TimeUnit.MILLISECONDS).join();
        Assertions.assertEquals(50, completed.get());
    }

}

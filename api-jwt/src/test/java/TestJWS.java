import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.mdproxy.jwt.*;

import java.time.Instant;
import java.util.Random;

public class TestJWS {


    @Test
    public void test256() {

        Random rand = new Random();
        byte[] key = new byte[32];
        rand.nextBytes(key);

        HashCodec<byte[]> codec = HashCodec.HS256(key);

        Instant issued = Instant.now();
        JWSSerializer.JWS jws = new JWTBuilder()
                .issuedAt(issued)
                .issuedBy("test")
                .signed(codec);

        String encoded = jws.asString().getOrThrow();
        JWT decrypted = JWSSerializer.read(encoded, KeySupplier.of(codec.getKey(), codec.getAlgorithm().getKeyType())).getOrThrow();

        Assertions.assertEquals(HashCodec.ALGORITHMS.getId(codec.getAlgorithm()), decrypted.header().getString("alg"));
        Assertions.assertEquals("JWT", decrypted.header().getString("typ"));
        Assertions.assertEquals("test", decrypted.getIssuer());
        Assertions.assertEquals(issued.getEpochSecond(), decrypted.getIssuedAt().getEpochSecond());

    }

}

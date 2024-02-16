package org.wallentines.mdproxy.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.midnightlib.registry.StringRegistry;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Random;

public class CryptCodec<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger("CryptCodec");
    protected final Algorithm<T> algorithm;
    protected final T key;

    protected CryptCodec(Algorithm<T> algorithm, T key) {
        this.algorithm = algorithm;
        this.key = key;
    }

    public Algorithm<T> getAlgorithm() {
        return algorithm;
    }

    public CryptOutput encrypt(byte[] data, byte[] iv, byte[] aad) {
        return algorithm.encode(key, data, iv, aad);
    }

    public byte[] decrypt(byte[] data, byte[] iv) {
        return algorithm.decode(key, data, iv);
    }

    public T getKey() {
        return key;
    }

    public byte[] getEncodedKey() {
        return algorithm.keyType.serialize(key).getOrThrow();
    }

    public static CryptCodec<CompoundKey> A128CBC_HS256(Random random) {

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        CompoundKey ck = AES_CBC_HMAC_SHA2.A128CBC_HS256.keyType.create(bytes).getOrThrow();

        return new CryptCodec<>(AES_CBC_HMAC_SHA2.A128CBC_HS256, ck);
    }

    public static CryptCodec<CompoundKey> A128CBC_HS256(CompoundKey key) {
        return new CryptCodec<>(AES_CBC_HMAC_SHA2.A128CBC_HS256, key);
    }

    public static CryptCodec<CompoundKey> A192CBC_HS384(Random random) {

        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        CompoundKey ck = AES_CBC_HMAC_SHA2.A128CBC_HS256.keyType.create(bytes).getOrThrow();

        return new CryptCodec<>(AES_CBC_HMAC_SHA2.A128CBC_HS256, ck);
    }

    public static CryptCodec<CompoundKey> A192CBC_HS384(CompoundKey key) {
        return new CryptCodec<>(AES_CBC_HMAC_SHA2.A192CBC_HS384, key);
    }

    public static CryptCodec<CompoundKey> A256CBC_HS512(Random random) {

        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        CompoundKey ck = AES_CBC_HMAC_SHA2.A128CBC_HS256.keyType.create(bytes).getOrThrow();

        return new CryptCodec<>(AES_CBC_HMAC_SHA2.A128CBC_HS256, ck);
    }
    public static CryptCodec<CompoundKey> A256CBC_HS512(CompoundKey key) {
        return new CryptCodec<>(AES_CBC_HMAC_SHA2.A256CBC_HS512, key);
    }

    public record CryptOutput(byte[] cipherText, byte[] authTag) { }


    public static abstract class Algorithm<T> {

        protected final int keyLength;
        protected final KeyType<T> keyType;

        protected Algorithm(int keyLength, KeyType<T> keyType) {
            this.keyLength = keyLength;
            this.keyType = keyType;
        }

        public KeyType<T> getKeyType() {
            return keyType;
        }

        public abstract CryptOutput encode(T key, byte[] bytes, byte[] iv, byte[] aac);
        public abstract byte[] decode(T key, byte[] bytes, byte[] iv);

        public static final StringRegistry<Algorithm<?>> REGISTRY = new StringRegistry<>();

        private static <T, A extends Algorithm<T>> A register(String key, A alg) {
            REGISTRY.register(key, alg);
            return alg;
        }

        public CryptCodec<T> createCodec(T key) {
            return new CryptCodec<>(this, key);
        }

        public CryptCodec<T> createCodec(ConfigSection header, KeySupplier supp) {
            return new CryptCodec<>(this, supp.getKey(header, keyType));
        }

        public CryptCodec<T> createCodec(byte[] encodedKey) {
            return new CryptCodec<>(this, keyType.create(encodedKey).getOrThrow());
        }

    }

    private static class AES_CBC_HMAC_SHA2 extends Algorithm<CompoundKey> {

        public AES_CBC_HMAC_SHA2(int keyLength, HashCodec.Algorithm hashAlg) {
            super(keyLength, CompoundKey.type(keyLength, KeyType.AES, hashAlg));
        }

        @Override
        public CryptOutput encode(CompoundKey key, byte[] data, byte[] iv, byte[] aad) {
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, key.crypt, new IvParameterSpec(iv));

                byte[] cipherText = cipher.doFinal(data);

                ByteBuffer nioBuffer = ByteBuffer.allocate(8);
                nioBuffer.order(ByteOrder.BIG_ENDIAN);
                nioBuffer.putLong(aad.length);

                byte[] auth = key.hash.hash(aad, iv, cipherText, nioBuffer.array());
                return new CryptOutput(cipherText, auth);

            } catch (GeneralSecurityException ex) {
                throw new IllegalArgumentException("Unable to encrypt data!", ex);
            }
        }

        @Override
        public byte[] decode(CompoundKey key, byte[] data, byte[] iv) {
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, key.crypt, new IvParameterSpec(iv));

                return cipher.doFinal(data);

            } catch (GeneralSecurityException ex) {
                throw new IllegalArgumentException("Unable to decrypt data!", ex);
            }
        }

        public static final AES_CBC_HMAC_SHA2 A128CBC_HS256 = Algorithm.register("A128CBC-HS256", new AES_CBC_HMAC_SHA2(32, HashCodec.HMAC.HS256));
        public static final AES_CBC_HMAC_SHA2 A192CBC_HS384 = Algorithm.register("A192CBC-HS384", new AES_CBC_HMAC_SHA2(48, HashCodec.HMAC.HS384));
        public static final AES_CBC_HMAC_SHA2 A256CBC_HS512 = Algorithm.register("A256CBC-HS512", new AES_CBC_HMAC_SHA2(64, HashCodec.HMAC.HS512));
    }

    public record CompoundKey(byte[] rawKey, SecretKey crypt, HashCodec<?> hash) {

        public static KeyType<CompoundKey> type(int keyLength, KeyType.Secret cryptType, HashCodec.Algorithm<?> hashAlg) {

            return new KeyType<>() {
                @Override
                public SerializeResult<CompoundKey> create(byte[] bytes) {

                    int halfLength = keyLength / 2;
                    SecretKey crypt = cryptType.create(Arrays.copyOfRange(bytes, 0, halfLength)).getOrThrow();
                    HashCodec<?> hash = hashAlg.createCodec(Arrays.copyOfRange(bytes, bytes.length - halfLength, bytes.length));

                    return SerializeResult.success(new CompoundKey(bytes, crypt, hash));
                }

                @Override
                public SerializeResult<byte[]> serialize(CompoundKey key) {
                    return SerializeResult.success(key.rawKey);
                }
            };
        }
    }

}

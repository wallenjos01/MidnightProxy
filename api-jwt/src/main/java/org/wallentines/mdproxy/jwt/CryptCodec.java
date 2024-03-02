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
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptCodec<T> {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger LOGGER = LoggerFactory.getLogger("CryptCodec");

    public static final StringRegistry<Algorithm<?>> ALGORITHMS = new StringRegistry<>();

    protected final Algorithm<T> algorithm;
    protected final T key;
    protected final byte[] iv;
    protected final SecureRandom random;

    public CryptCodec(Algorithm<T> algorithm, T key) {
        this.algorithm = algorithm;
        this.key = key;
        this.random = RANDOM;

        this.iv = new byte[algorithm.ivLength];
        RANDOM.nextBytes(iv);
    }

    public CryptCodec(Algorithm<T> algorithm, T key, byte[] iv) {
        this.algorithm = algorithm;
        this.key = key;
        this.iv = iv;
        this.random = RANDOM;
    }

    public CryptCodec(Algorithm<T> algorithm, T key, SecureRandom random) {
        this.algorithm = algorithm;
        this.key = key;
        this.random = random;

        this.iv = new byte[algorithm.ivLength];
        random.nextBytes(iv);
    }


    public CryptCodec(Algorithm<T> algorithm, T key, byte[] iv, SecureRandom random) {
        this.algorithm = algorithm;
        this.key = key;
        this.iv = iv;
        this.random = random;
    }

    public byte[] getIV() {
        return iv;
    }

    public Algorithm<T> getAlgorithm() {
        return algorithm;
    }

    public CryptOutput encrypt(byte[] data, byte[] aad) {
        return algorithm.encode(key, data, iv, aad);
    }

    public byte[] decrypt(byte[] data) {
        return algorithm.decode(key, data, iv);
    }

    public T getKey() {
        return key;
    }

    public byte[] getEncodedKey() {
        return algorithm.keyType.serialize(key).getOrThrow();
    }

    public static CryptCodec<CompoundKey> A128CBC_HS256() {

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        CompoundKey ck = ALG_A128CBC_HS256.keyType.create(bytes).getOrThrow();

        return new CryptCodec<>(ALG_A128CBC_HS256, ck);
    }

    public static CryptCodec<CompoundKey> A128CBC_HS256(CompoundKey key) {
        return new CryptCodec<>(ALG_A128CBC_HS256, key);
    }

    public static CryptCodec<CompoundKey> A192CBC_HS384() {

        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        CompoundKey ck = ALG_A128CBC_HS256.keyType.create(bytes).getOrThrow();

        return new CryptCodec<>(ALG_A128CBC_HS256, ck);
    }

    public static CryptCodec<CompoundKey> A192CBC_HS384(CompoundKey key) {
        return new CryptCodec<>(ALG_A192CBC_HS384, key);
    }

    public static CryptCodec<CompoundKey> A256CBC_HS512() {

        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        CompoundKey ck = ALG_A128CBC_HS256.keyType.create(bytes).getOrThrow();

        return new CryptCodec<>(ALG_A128CBC_HS256, ck);
    }
    public static CryptCodec<CompoundKey> A256CBC_HS512(CompoundKey key) {
        return new CryptCodec<>(ALG_A256CBC_HS512, key);
    }

    public record CryptOutput(byte[] cipherText, byte[] authTag) { }


    public static abstract class Algorithm<T> {

        protected final int keyLength;
        protected final int ivLength;
        protected final KeyType<T> keyType;

        protected Algorithm(int keyLength, int ivLength, KeyType<T> keyType) {
            this.keyLength = keyLength;
            this.keyType = keyType;
            this.ivLength = ivLength;
        }

        public KeyType<T> getKeyType() {
            return keyType;
        }

        public abstract CryptOutput encode(T key, byte[] bytes, byte[] iv, byte[] aac);
        public abstract byte[] decode(T key, byte[] bytes, byte[] iv);



        public CryptCodec<T> createCodec(T key) {
            return new CryptCodec<>(this, key);
        }

        public CryptCodec<T> createCodec(ConfigSection header, KeySupplier supp, byte[] iv) {
            return new CryptCodec<>(this, supp.getKey(header, keyType), iv);
        }


        public CryptCodec<T> createCodec(ConfigSection header, KeySupplier supp) {
            return new CryptCodec<>(this, supp.getKey(header, keyType));
        }

        public CryptCodec<T> createCodec(byte[] encodedKey, byte[] iv) {
            return new CryptCodec<>(this, keyType.create(encodedKey).getOrThrow(), iv);
        }

        public CryptCodec<T> createCodec(byte[] encodedKey) {
            return new CryptCodec<>(this, keyType.create(encodedKey).getOrThrow());
        }


    }

    private static class AES_CBC_HMAC_SHA2 extends Algorithm<CompoundKey> {

        public AES_CBC_HMAC_SHA2(int keyLength, HashCodec.Algorithm<?> hashAlg) {
            super(keyLength, 16, CompoundKey.type(keyLength, KeyType.AES, hashAlg));
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


    public static final Algorithm<CompoundKey> ALG_A128CBC_HS256 = new AES_CBC_HMAC_SHA2(32, HashCodec.ALG_HS256);
    public static final Algorithm<CompoundKey> ALG_A192CBC_HS384 = new AES_CBC_HMAC_SHA2(48, HashCodec.ALG_HS384);
    public static final Algorithm<CompoundKey> ALG_A256CBC_HS512 = new AES_CBC_HMAC_SHA2(64, HashCodec.ALG_HS512);

    static {
        ALGORITHMS.register("A128CBC-HS256", ALG_A128CBC_HS256);
        ALGORITHMS.register("A192CBC-HS384", ALG_A192CBC_HS384);
        ALGORITHMS.register("A256CBC-HS512", ALG_A256CBC_HS512);
    }

}

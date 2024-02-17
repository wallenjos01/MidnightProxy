package org.wallentines.mdproxy.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.midnightlib.registry.StringRegistry;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.*;

public class KeyCodec<E extends Key, D extends Key> {

    public static final StringRegistry<Algorithm<?,?>> ALGORITHMS = new StringRegistry<>();;
    private static final Logger LOGGER = LoggerFactory.getLogger("KeyCodec");

    private final E encKey;
    private final D decKey;
    private final Algorithm<E,D> alg;

    protected KeyCodec(Algorithm<E,D> alg, E encKey, D decKey) {
        this.alg = alg;
        this.encKey = encKey;
        this.decKey = decKey;
    }
    public Algorithm<E,D> getAlgorithm() { return alg; }

    public boolean canEncode() {
        return encKey != null;
    }
    public boolean canDecode() {
        return decKey != null;
    }
    public byte[] encode(byte[] bytes) {
        return alg.encode(encKey, bytes);
    }
    public byte[] decode(byte[] bytes) {
        return alg.decode(decKey, bytes);
    }

    public E getEncryptionKey() {
        return encKey;
    }

    public D getDecryptionKey() {
        return decKey;
    }

    public static KeyCodec<DummyKey, DummyKey> direct() {
        DummyKey value = new DummyKey();
        return new KeyCodec<>(ALG_DIRECT, value, value);
    }


    public static KeyCodec<PublicKey, PrivateKey> RSA_OAEP(KeyPair kp) {
        return new KeyCodec<>(ALG_RSA_OAEP, kp.getPublic(), kp.getPrivate());
    }

    public static KeyCodec<PublicKey, PrivateKey> RSA_OAEP(PublicKey key) {
        return new KeyCodec<>(ALG_RSA_OAEP, key, null);
    }

    public static KeyCodec<PublicKey, PrivateKey> RSA_OAEP(PrivateKey key) {
        return new KeyCodec<>(ALG_RSA_OAEP, null, key);
    }

    public static KeyCodec<SecretKey, SecretKey> A128KW(byte[] key) {
        return A128KW(KeyType.AES.create(key).getOrThrow());
    }

    public static KeyCodec<SecretKey, SecretKey> A128KW(SecretKey key) {
        return new KeyCodec<>(ALG_A128KW, key, key);
    }

    public static KeyCodec<SecretKey, SecretKey> A192KW(byte[] key) {
        return A192KW(KeyType.AES.create(key).getOrThrow());
    }

    public static KeyCodec<SecretKey, SecretKey> A192KW(SecretKey key) {
        return new KeyCodec<>(ALG_A192KW, key, key);
    }

    public static KeyCodec<SecretKey, SecretKey> A256KW(byte[] key) {
        return A256KW(KeyType.AES.create(key).getOrThrow());
    }

    public static KeyCodec<SecretKey, SecretKey> A256KW(SecretKey key) {
        return new KeyCodec<>(ALG_A256KW, key, key);
    }


    public static class DummyKey implements Key {

        @Override
        public String getAlgorithm() { return null; }

        @Override
        public String getFormat() { return null; }
        @Override
        public byte[] getEncoded() { return new byte[0]; }
    }

    public static class Algorithm<E extends Key, D extends Key> {

        private final KeyType<E> encKeyType;
        private final KeyType<D> decKeyType;
        private final String algorithm;

        public Algorithm(KeyType<E> encKeyType, KeyType<D> decKeyType, String algorithm) {
            this.encKeyType = encKeyType;
            this.decKeyType = decKeyType;
            this.algorithm = algorithm;
        }

        public byte[] encode(E key, byte[] data) {
            if(key == null) throw new IllegalStateException("Unable to encode key with this codec!");
            try {
                Cipher cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                return cipher.doFinal(data);
            } catch (GeneralSecurityException ex) {
                throw new IllegalArgumentException("Unable to encode key!", ex);
            }
        }

        public byte[] decode(D key, byte[] data) {
            if(key == null) throw new IllegalStateException("Unable to decode key with this codec!");
            try {
                Cipher cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.DECRYPT_MODE, key);
                return cipher.doFinal(data);
            } catch (GeneralSecurityException ex) {
                throw new IllegalArgumentException("Unable to decode key!", ex);
            }
        }

        public KeyType<E> getEncryptionKeyType() {
            return encKeyType;
        }

        public KeyType<D> getDecryptionKeyType() {
            return decKeyType;
        }

        public KeyCodec<E,D> createCodec(ConfigSection header, KeySupplier supp) {

            E encKey = supp.getKey(header, encKeyType);
            D decKey = supp.getKey(header, decKeyType);
            return new KeyCodec<>(this, encKey, decKey);
        }
    }


    public static final Algorithm<PublicKey, PrivateKey> ALG_RSA_OAEP = new Algorithm<>(KeyType.RSA_PUBLIC, KeyType.RSA_PRIVATE, "RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
    public static final Algorithm<SecretKey, SecretKey> ALG_A128KW = new Algorithm<>(KeyType.AES, KeyType.AES, "AESWrap");
    public static final Algorithm<SecretKey, SecretKey> ALG_A192KW = new Algorithm<>(KeyType.AES, KeyType.AES, "AESWrap");
    public static final Algorithm<SecretKey, SecretKey> ALG_A256KW = new Algorithm<>(KeyType.AES, KeyType.AES, "AESWrap");
    public static final Algorithm<DummyKey, DummyKey> ALG_DIRECT = new Algorithm<>(null,null,null) {
        @Override
        public byte[] encode(DummyKey key, byte[] data) {
            return new byte[0];
        }
        @Override
        public byte[] decode(DummyKey key, byte[] data) {
            return new byte[0];
        }
    };

    static {
        ALGORITHMS.register("RSA-OAEP", ALG_RSA_OAEP);
        ALGORITHMS.register("A128KW", ALG_A128KW);
        ALGORITHMS.register("A192KW", ALG_A192KW);
        ALGORITHMS.register("A256KW", ALG_A256KW);
        ALGORITHMS.register("dir", ALG_DIRECT);
    }
}

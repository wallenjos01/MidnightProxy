package org.wallentines.mdproxy.jwt;

import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.midnightlib.registry.Registry;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public class HashCodec<T> {

    public static final Registry<String, Algorithm<?>> ALGORITHMS = Registry.createStringRegistry();
    private final Algorithm<T> alg;
    private final T key;

    public HashCodec(Algorithm<T> alg, T key) {
        this.alg = alg;
        this.key = key;
    }

    public Algorithm<T> getAlgorithm() {
        return alg;
    }

    public byte[] hash(byte[]... input) {
        return alg.hash(key, input);
    }

    public T getKey() {
        return key;
    }

    public static HashCodec<Void> none() {
        return new HashCodec<>(ALG_NONE, null);
    }


    public static HashCodec<byte[]> HS256(byte[] secret) {
        return ALG_HS256.createCodec(secret);
    }

    public static HashCodec<byte[]> HS384(byte[] secret) {
        return ALG_HS384.createCodec(secret);
    }

    public static HashCodec<byte[]> HS512(byte[] secret) {
        return ALG_HS512.createCodec(secret);
    }



    public static abstract class Algorithm<T> {

        protected final KeyType<T> keyType;

        protected Algorithm(KeyType<T> keyType) {
            this.keyType = keyType;
        }

        public abstract byte[] hash(T key, byte[]... inputs);

        public HashCodec<T> createCodec(ConfigSection header, KeySupplier keySupplier) {

            return new HashCodec<>(this, keySupplier.getKey(header, keyType));
        }


        public HashCodec<T> createCodec(byte[] key) {
            return new HashCodec<>(this, keyType.create(key).getOrThrow());
        }

        public KeyType<T> getKeyType() {
            return keyType;
        }

    }

    public static class HMAC extends Algorithm<byte[]> {

        private final String algorithm;

        protected HMAC(String alg, KeyType<byte[]> keyType) {
            super(keyType);
            this.algorithm = alg;
        }

        @Override
        public byte[] hash(byte[] key, byte[]... input) {

            try {
                Mac mac = Mac.getInstance(algorithm);
                SecretKey secret = new SecretKeySpec(key, mac.getAlgorithm());
                mac.init(secret);
                for(byte[] bs : input) {
                    mac.update(bs);
                }
                return mac.doFinal();
            } catch (GeneralSecurityException ex) {
                throw new IllegalStateException("Unable to initialize HMAC!");
            }
        }
    }
    public static final Algorithm<Void> ALG_NONE = new Algorithm<>(null) {
        @Override
        public byte[] hash(Void key, byte[]... inputs) {
            return new byte[0];
        }
    };
    public static final HMAC ALG_HS256 = new HMAC("HmacSHA256", KeyType.HMAC);
    public static final HMAC ALG_HS384 = new HMAC("HmacSHA384", KeyType.HMAC);
    public static final HMAC ALG_HS512 = new HMAC("HmacSHA512", KeyType.HMAC);

    static {
        ALGORITHMS.register("none", ALG_NONE);
        ALGORITHMS.register("HS256", ALG_HS256);
        ALGORITHMS.register("HS384", ALG_HS384);
        ALGORITHMS.register("HS512", ALG_HS512);
    }
}

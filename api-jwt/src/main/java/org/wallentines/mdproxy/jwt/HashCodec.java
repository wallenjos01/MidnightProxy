package org.wallentines.mdproxy.jwt;

import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.midnightlib.registry.StringRegistry;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public class HashCodec<T> {

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
        return new HashCodec<>(Algorithm.NONE, null);
    }


    public static HashCodec<byte[]> HS256(byte[] secret) {
        return HMAC.HS256.createCodec(secret);
    }

    public static HashCodec<byte[]> HS384(byte[] secret) {
        return HMAC.HS384.createCodec(secret);
    }

    public static HashCodec<byte[]> HS512(byte[] secret) {
        return HMAC.HS512.createCodec(secret);
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

        public static final StringRegistry<Algorithm<?>> REGISTRY = new StringRegistry<>();

        private static <T, A extends Algorithm<T>> A register(String key, A alg) {
            REGISTRY.register(key, alg);
            return alg;
        }

        public static final Algorithm<Void> NONE = new Algorithm<>(null) {
            @Override
            public byte[] hash(Void key, byte[]... inputs) {
                return new byte[0];
            }
        };


    }

    public static class HMAC extends Algorithm<byte[]> {

        private final Mac mac;

        protected HMAC(String alg, KeyType<byte[]> keyType) {
            super(keyType);
            try {
                this.mac = Mac.getInstance(alg);
            } catch (GeneralSecurityException ex) {
                throw new IllegalStateException("Unable to find HMAC algorithm: " + alg);
            }
        }

        @Override
        public byte[] hash(byte[] key, byte[]... input) {

            SecretKey secret = new SecretKeySpec(key, mac.getAlgorithm());
            try {
                mac.init(secret);
            } catch (GeneralSecurityException ex) {
                throw new IllegalStateException("Unable to initialize HMAC!");
            }
            for(byte[] bs : input) {
                mac.update(bs);
            }

            return mac.doFinal();
        }

        public static final HMAC HS256 = Algorithm.register("HS256", new HMAC("HmacSHA256", KeyType.HMAC));
        public static final HMAC HS384 = Algorithm.register("HS384", new HMAC("HmacSHA384", KeyType.HMAC));
        public static final HMAC HS512 = Algorithm.register("HS512", new HMAC("HmacSHA512", KeyType.HMAC));
    }

}

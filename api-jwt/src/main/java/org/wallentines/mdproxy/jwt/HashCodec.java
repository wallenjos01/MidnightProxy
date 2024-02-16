package org.wallentines.mdproxy.jwt;

import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.Functions;
import org.wallentines.midnightlib.registry.StringRegistry;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
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


    public static HashCodec<SecretKey> HS256(byte[] secret) {
        return new HashCodec<>(HMAC.HS256, KeyType.HMAC256.create(secret).getOrThrow());
    }

    public static HashCodec<SecretKey> HS256(SecretKey key) {
        return new HashCodec<>(HMAC.HS256, key);
    }

    public static HashCodec<SecretKey> HS384(byte[] secret) {
        return new HashCodec<>(HMAC.HS384, KeyType.HMAC384.create(secret).getOrThrow());
    }

    public static HashCodec<SecretKey> HS384(SecretKey key) {
        return new HashCodec<>(HMAC.HS384, key);
    }

    public static HashCodec<SecretKey> HS512(byte[] secret) {
        return new HashCodec<>(HMAC.HS512, KeyType.HMAC512.create(secret).getOrThrow());
    }

    public static HashCodec<SecretKey> HS512(SecretKey key) {
        return new HashCodec<>(HMAC.HS512, key);
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

    public static class HMAC extends Algorithm<SecretKey> {

        private final Mac mac;

        protected HMAC(KeyType.Secret keyType) {
            super(keyType);
            try {
                this.mac = Mac.getInstance(keyType.getAlgorithm());
            } catch (GeneralSecurityException ex) {
                throw new IllegalStateException("Unable to find HMAC algorithm: " + keyType.getAlgorithm());
            }
        }

        @Override
        public byte[] hash(SecretKey key, byte[]... input) {

            try {
                mac.init(key);
            } catch (GeneralSecurityException ex) {
                throw new IllegalStateException("Unable to initialize HMAC!");
            }
            for(byte[] bs : input) {
                mac.update(bs);
            }

            return mac.doFinal();
        }

        public static final HMAC HS256 = Algorithm.register("HS256", new HMAC(KeyType.HMAC256));
        public static final HMAC HS384 = Algorithm.register("HS384", new HMAC(KeyType.HMAC256));
        public static final HMAC HS512 = Algorithm.register("HS512", new HMAC(KeyType.HMAC256));
    }

}

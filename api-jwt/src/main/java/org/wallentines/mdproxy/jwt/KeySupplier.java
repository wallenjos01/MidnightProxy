package org.wallentines.mdproxy.jwt;

import org.wallentines.mdcfg.ConfigSection;

import java.security.Key;

public interface KeySupplier {

    <T> T getKey(ConfigSection joseHeader, KeyType<T> type);


    static <T> KeySupplier of(HashCodec<T> codec) {
        return of(codec.getKey(), codec.getAlgorithm().getKeyType());
    }

    static <T> KeySupplier of(CryptCodec<T> codec) {
        return of(codec.getKey(), codec.getAlgorithm().getKeyType());
    }

    static <D extends Key, E extends Key> KeySupplier of(KeyCodec<D, E> codec) {
        return of(codec.getDecryptionKey(), codec.getAlgorithm().getDecryptionKeyType());
    }


    static <T> KeySupplier of(T key, KeyType<T> type) {
        return new KeySupplier() {
            @SuppressWarnings("unchecked")
            @Override
            public <T2> T2 getKey(ConfigSection joseHeader, KeyType<T2> type2) {
                if(type != type2) {
                    return null;
                }
                return (T2) key;
            }
        };
    }

    static KeySupplier fromHeader(KeyStore store) {

        return fromHeader(store, null, null);
    }

    static KeySupplier fromHeader(KeyStore store, KeyType<?> requiredType, String name) {

        return new KeySupplier() {
            @Override
            public <T> T getKey(ConfigSection joseHeader, KeyType<T> type) {

                if(requiredType != null && requiredType != type) {
                    return null;
                }

                String kid = name;
                if(kid == null) kid = joseHeader.getOrDefault("kid", "default");

                KeyCodec.Algorithm<?,?> alg = KeyCodec.ALGORITHMS.get(joseHeader.getString("alg"));
                if(alg == null) return null;

                if(alg.getDecryptionKeyType() != type) return null;
                return store.getKey(kid, type);

            }
        };
    }

}

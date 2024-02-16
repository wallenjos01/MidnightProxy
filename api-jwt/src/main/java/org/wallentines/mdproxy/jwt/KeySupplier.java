package org.wallentines.mdproxy.jwt;

import org.wallentines.mdcfg.ConfigSection;

public interface KeySupplier {

    <T> T getKey(ConfigSection joseHeader, KeyType<T> type);


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

    static KeySupplier fromHeader(FileKeyStore store) {

        return new KeySupplier() {
            @Override
            public <T> T getKey(ConfigSection joseHeader, KeyType<T> type) {

                String kid = joseHeader.getOrDefault("kid", "default");

                KeyCodec.Algorithm<?,?> alg = KeyCodec.Algorithm.REGISTRY.get(joseHeader.getString("alg"));
                if(alg == null) return null;

                if(alg.getDecryptionKeyType() != type) return null;
                return store.getKey(kid, type);

            }
        };
    }

}

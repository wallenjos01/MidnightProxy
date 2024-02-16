package org.wallentines.mdproxy.jwt;

import java.util.HashMap;
import java.util.Map;

public interface KeyStore {

    <T> T getKey(String name, KeyType<T> type);

    <T> void setKey(String name, KeyType<T> type, T key);

    <T> void clearKey(String name, KeyType<T> type);

    class KeyRegistry<T> {
        private final KeyType<T> type;
        private final Map<String, T> keys = new HashMap<>();

        public KeyRegistry(KeyType<T> type) {
            this.type = type;
        }
        KeyType<T> getType() {
            return type;
        }
        boolean hasKey(String key) {
            return keys.containsKey(key);
        }
        T getKey(String key) {
            return keys.get(key);
        }
        void setKey(String name, T key) {
            this.keys.put(name, key);
        }
        T clearKey(String name) {
            return this.keys.remove(name);
        }

    }

}

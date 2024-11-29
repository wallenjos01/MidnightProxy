package org.wallentines.mdproxy.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.serializer.SerializeResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FileKeyStore implements KeyStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("FileKeyStore");
    private static final int HMAC_LENGTH = 32;
    private final Map<KeyType<?>, String> extensions;
    private final Map<KeyType<?>, KeyRegistry<?>> allKeys = new HashMap<>();
    private final Path keyFolder;

    public static final Map<KeyType<?>, String> DEFAULT_TYPES = Map.of(
            KeyType.HMAC, "key",
            KeyType.AES, "aes",
            KeyType.RSA_PUBLIC, "pub",
            KeyType.RSA_PRIVATE, "rsa"
    );

    public FileKeyStore(Path keyFolder, Map<KeyType<?>, String> validKeyTypes) {
        this.keyFolder = keyFolder;
        this.extensions = Map.copyOf(validKeyTypes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getKey(String kid, KeyType<T> type) {

        if(!extensions.containsKey(type)) {
            LOGGER.warn("Requested key with unknown type {}", type);
            return null;
        }

        KeyRegistry<?> ureg = allKeys.computeIfAbsent(type, KeyRegistry::new);
        if(ureg.getType() != type) {
            LOGGER.warn("Unable to find registry for type {}", type);
            return null;
        }

        KeyRegistry<T> reg = (KeyRegistry<T>) ureg;
        if(!reg.hasKey(kid)) {

            Path keyFile = keyFolder.resolve(kid + "." + extensions.get(type));
            byte[] value;
            try(
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    InputStream fis = Files.newInputStream(keyFile)
            ) {

                byte[] buffer = new byte[HMAC_LENGTH];
                int read;
                while((read = fis.read(buffer)) > -1) {
                    bos.write(buffer, 0, read);
                }
                value = bos.toByteArray();

            } catch (IOException ex) {
                return null;
            }

            SerializeResult<T> key = type.create(value);
            if(!key.isComplete()) {
                LOGGER.warn("Unable to read key! {}", key.getError());
                return null;
            }

            reg.setKey(kid, key.getOrThrow());
        }

        return reg.getKey(kid);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void setKey(String name, KeyType<T> type, T key) {

        if(!extensions.containsKey(type)) {
            LOGGER.warn("Attempt to register key with unknown type {}", type);
            return;
        }

        KeyRegistry<?> ureg = allKeys.computeIfAbsent(type, KeyRegistry::new);
        if(ureg.getType() != type) {
            LOGGER.warn("Unable to create registry for type {}", type);
            return;
        }

        KeyRegistry<T> reg = (KeyRegistry<T>) ureg;
        reg.setKey(name, key);

        // Save key file
        Path f = keyFolder.resolve(name + "." + extensions.get(type));
        try(OutputStream fos = Files.newOutputStream(f)) {
            fos.write(type.serialize(key).getOrThrow());
        } catch (Exception ex) {
            LOGGER.warn("Unable to save key {}!", name, ex);
        }
    }

    @Override
    public <T> void clearKey(String name, KeyType<T> type) {

        if(!extensions.containsKey(type) || !allKeys.containsKey(type)) {
            return;
        }
        if(allKeys.get(type).clearKey(name) != null) {
            Path p = keyFolder.resolve(name + "." + extensions.get(type));
            try {
                if (!Files.deleteIfExists(p)) {
                    LOGGER.warn("Unable to delete key file {}", p.getFileName());
                }
            } catch (Exception ex) {
                LOGGER.warn("An error occurred while deleting key file {}", p.getFileName(), ex);
            }
        }
    }

    @Override
    public KeySupplier supplier(String name) {
        return new KeySupplier() {
            @Override
            public <T> T getKey(ConfigSection joseHeader, KeyType<T> type) {
                return FileKeyStore.this.getKey(name, type);
            }
        };
    }

    @Override
    public <K> KeySupplier supplier(String name, KeyType<K> requiredType) {
        return new KeySupplier() {
            @Override
            public <T> T getKey(ConfigSection joseHeader, KeyType<T> type) {
                if(requiredType != type) return null;
                return FileKeyStore.this.getKey(name, type);
            }
        };
    }

}

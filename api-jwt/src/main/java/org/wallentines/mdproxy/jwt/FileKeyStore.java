package org.wallentines.mdproxy.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.serializer.SerializeResult;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FileKeyStore implements KeyStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("FileKeyStore");
    private static final int HMAC_LENGTH = 32;
    private final Map<KeyType<?>, String> extensions;
    private final Map<KeyType<?>, KeyRegistry<?>> allKeys = new HashMap<>();
    private final File keyFolder;

    public static final Map<KeyType<?>, String> DEFAULT_TYPES = Map.of(
            KeyType.HMAC, "key",
            KeyType.AES, "aes",
            KeyType.RSA_PUBLIC, "pub",
            KeyType.RSA_PRIVATE, "rsa"
    );

    public FileKeyStore(File keyFolder, Map<KeyType<?>, String> validKeyTypes) {
        this.keyFolder = keyFolder;
        this.extensions = Map.copyOf(validKeyTypes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getKey(String kid, KeyType<T> type) {

        if(!extensions.containsKey(type)) {
            LOGGER.warn("Requested key with unknown type " + type);
            return null;
        }

        KeyRegistry<?> ureg = allKeys.computeIfAbsent(type, KeyRegistry::new);
        if(ureg.getType() != type) {
            LOGGER.warn("Unable to create registry for type " + type);
            return null;
        }

        KeyRegistry<T> reg = (KeyRegistry<T>) ureg;
        if(!reg.hasKey(kid)) {

            File keyFile = new File(keyFolder, kid + "." + extensions.get(type));
            byte[] value;
            try(
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    FileInputStream fis = new FileInputStream(keyFile);
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
                LOGGER.warn("Unable to read key! " + key.getError());
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
            LOGGER.warn("Requested key with unknown type " + type);
            return;
        }

        KeyRegistry<?> ureg = allKeys.computeIfAbsent(type, KeyRegistry::new);
        if(ureg.getType() != type) {
            LOGGER.warn("Unable to create registry for type " + type);
            return;
        }

        KeyRegistry<T> reg = (KeyRegistry<T>) ureg;
        reg.setKey(name, key);

        // Save key file
        File f = new File(keyFolder, name + "." + extensions.get(type));
        try(FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(type.serialize(key).getOrThrow());
        } catch (Exception ex) {
            LOGGER.warn("Unable to save key " + name + "!", ex);
        }
    }

    @Override
    public <T> void clearKey(String name, KeyType<T> type) {

        if(!extensions.containsKey(type) || !allKeys.containsKey(type)) {
            return;
        }
        if(allKeys.get(type).clearKey(name) != null) {
            File f = new File(keyFolder, name + "." + extensions.get(type));
            if(!f.delete()) {
                LOGGER.warn("Unable to delete key file " + f.getName());
            }
        }
    }

}

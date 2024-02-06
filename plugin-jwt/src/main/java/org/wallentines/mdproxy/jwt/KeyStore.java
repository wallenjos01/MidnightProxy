package org.wallentines.mdproxy.jwt;

import java.io.*;
import java.util.HashMap;
import java.util.Random;

public class KeyStore {

    private static final int KEY_SIZE = 128;
    private final File folder;
    private final Random random;
    private final HashMap<String, byte[]> keys = new HashMap<>();

    public KeyStore(File folder) {
        this.folder = folder;
        this.random = new Random();

        if(!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Unable to create keystore folder!");
        }
        File[] files = folder.listFiles();
        if(files == null || files.length == 0) {
            generateKey("default");
        }
    }

    public void generateKey(String name) {

        File out = new File(folder, name + ".key");

        byte[] key = new byte[KEY_SIZE];
        random.nextBytes(key);

        try(FileOutputStream fos = new FileOutputStream(out)) {

            fos.write(key);

        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create key!", ex);
        }

        keys.put(name, key);
    }

    public byte[] getSecret(String name) {
        return keys.computeIfAbsent(name, k -> {

            File f = new File(folder, name + ".key");
            try(
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                FileInputStream fis = new FileInputStream(f))
            {

                byte[] buffer = new byte[KEY_SIZE];
                int bytesRead;
                while((bytesRead = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }

                fis.close();
                bos.close();

                return bos.toByteArray();

            } catch (IOException ex) {
                return null;
            }

        });
    }

    public void clearKey(String name) {

        keys.remove(name);
        File f = new File(folder, name + ".key");
        if(f.exists() && !f.delete()) {
            throw new IllegalStateException("Unable to delete key at " + f + "!");
        }

    }

}

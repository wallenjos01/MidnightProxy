package org.wallentines.mdproxy;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IconCacheImpl implements IconCache {

    private static final Logger LOGGER = LoggerFactory.getLogger("IconCacheImpl");
    private final File searchDirectory;
    private final Map<String, String> iconData;
    private final ArrayDeque<String> cache;
    private final int cacheSize;
    private final Base64.Encoder encoder = Base64.getEncoder();

    public IconCacheImpl(File file, int cacheSize) {
        this.searchDirectory = file;
        this.cacheSize = cacheSize;
        this.iconData = new ConcurrentHashMap<>();
        this.cache = new ArrayDeque<>(cacheSize);
    }

    @Override
    @Nullable
    public String getIconB64(String name) {

        return iconData.computeIfAbsent(name, k -> {

            File file = new File(searchDirectory, k + ".png");
            String data;
            try(FileInputStream fis = new FileInputStream(file)) {
                data = "data:image/png;base64," + encoder.encodeToString(fis.readAllBytes());
            } catch (IOException ex) {
                LOGGER.warn("An exception occurred while loading an icon!", ex);
                return null;
            }

            if(cache.size() == cacheSize) {
                iconData.remove(cache.pop());
            }
            cache.push(k);

            return data;
        });
    }
}

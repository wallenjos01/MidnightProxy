package org.wallentines.mdproxy.sql;

import org.wallentines.mcore.MidnightCoreAPI;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.sql.DatabasePreset;
import org.wallentines.mdcfg.sql.PresetRegistry;
import org.wallentines.mdproxy.Proxy;
import org.wallentines.mdproxy.plugin.Plugin;

import java.io.File;

public class SQLPlugin implements Plugin {

    private static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("repository", new ConfigSection()
                    .with("type", "maven")
                    .with("folder", "config/sql/drivers"))
            .with("presets", new ConfigSection()
                    .with("default",
                            new DatabasePreset("h2", "config/sql/h2", null, null, null, null, new ConfigSection()),
                            DatabasePreset.SERIALIZER
                    )
            );

    private PresetRegistry registry;

    @Override
    public void initialize(Proxy proxy) {

        File configFolder = MidnightCoreAPI.GLOBAL_CONFIG_DIRECTORY.get().resolve("whitelist").toFile();
        if(!configFolder.isDirectory() && !configFolder.mkdirs()) {
            throw new IllegalStateException("Unable to create config directory!");
        }
        FileWrapper<ConfigObject> config = MidnightCoreAPI.FILE_CODEC_REGISTRY.findOrCreate(ConfigContext.INSTANCE, "config", configFolder, DEFAULT_CONFIG);
        registry = PresetRegistry.SERIALIZER.deserialize(ConfigContext.INSTANCE, config.getRoot()).getOrThrow();
    }

    public PresetRegistry getRegistry() {
        return registry;
    }
}

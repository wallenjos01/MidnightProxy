package org.wallentines.mdproxy.sql;

import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.sql.DatabasePreset;
import org.wallentines.mdcfg.sql.PresetRegistry;
import org.wallentines.mdproxy.Proxy;
import org.wallentines.mdproxy.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

        Path configFolder = proxy.getPluginManager().configFolder().resolve("whitelist");
        try { Files.createDirectories(configFolder); } catch (IOException e) {
            throw new RuntimeException("Could not create whitelist directory", e);
        }

        FileWrapper<ConfigObject> config = proxy.fileCodecRegistry().findOrCreate(ConfigContext.INSTANCE, "config", configFolder, DEFAULT_CONFIG);
        registry = PresetRegistry.SERIALIZER.deserialize(ConfigContext.INSTANCE, config.getRoot()).getOrThrow();
    }

    public PresetRegistry getRegistry() {
        return registry;
    }
}

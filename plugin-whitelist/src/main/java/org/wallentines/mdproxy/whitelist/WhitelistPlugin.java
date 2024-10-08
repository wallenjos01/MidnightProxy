package org.wallentines.mdproxy.whitelist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.MidnightCoreAPI;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdproxy.Proxy;
import org.wallentines.mdproxy.plugin.Plugin;
import org.wallentines.mdproxy.requirement.ConnectionCheckType;
import org.wallentines.midnightlib.registry.Registry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WhitelistPlugin implements Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("WhitelistPlugin");
    private static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("lists", new ConfigSection());
    private final Registry<String, Whitelist> lists = Registry.createStringRegistry();
    private FileWrapper<ConfigObject> config;

    @Override
    public void initialize(Proxy proxy) {

        Path configFolder = MidnightCoreAPI.GLOBAL_CONFIG_DIRECTORY.get().resolve("whitelist");
        try { Files.createDirectories(configFolder); } catch (IOException e) {
            throw new RuntimeException("Could not create config directory", e);
        }

        config = MidnightCoreAPI.FILE_CODEC_REGISTRY.findOrCreate(ConfigContext.INSTANCE, "config", configFolder, DEFAULT_CONFIG);
        ConnectionCheckType.REGISTRY.tryRegister("whitelist", WhitelistCheck.TYPE);

        reload();
    }

    public Registry<String, Whitelist> getLists() {
        return lists;
    }

    public void reload() {

        lists.clear();
        config.load();

        ConfigSection listSec = config.getRoot().asSection().getSection("lists");
        for(String key : listSec.getKeys()) {

            SerializeResult<Whitelist> out = Whitelist.SERIALIZER.deserialize(ConfigContext.INSTANCE, listSec.get(key));
            if(!out.isComplete()) {
                LOGGER.warn("Unable to deserialize a whitelist with key " + key + "! " + out.getError());
            }

            lists.register(key, out.getOrThrow());
        }
    }

}

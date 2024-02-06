package org.wallentines.mdproxy.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.MidnightCoreAPI;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdproxy.Proxy;
import org.wallentines.mdproxy.plugin.Plugin;
import org.wallentines.mdproxy.requirement.ConnectionCheck;
import org.wallentines.mdproxy.requirement.ConnectionRequirement;

import java.io.File;

public class JWTPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("JWTPlugin");
    private static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("key_store_path", "keystore");

    private KeyStore keyStore;

    @Override
    public void initialize(Proxy proxy) {

        File configFolder = MidnightCoreAPI.GLOBAL_CONFIG_DIRECTORY.get().resolve("jwt").toFile();
        if(!configFolder.isDirectory() && !configFolder.mkdirs()) {
            throw new IllegalStateException("Unable to create config directory!");
        }

        FileWrapper<ConfigObject> config = MidnightCoreAPI.FILE_CODEC_REGISTRY.findOrCreate(ConfigContext.INSTANCE, "config", configFolder, DEFAULT_CONFIG);
        keyStore = new KeyStore(new File(configFolder, config.getRoot().asSection().getString("key_store_path")));

        proxy.getCommands().register("jwt", new JWTCommand());
        ConnectionRequirement.REGISTRY.register("jwt", ConnectionCheck.forClass(JWTCheck.class, JWTCheck.SERIALIZER));

        LOGGER.info("JWT Plugin enabled!");
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }
}

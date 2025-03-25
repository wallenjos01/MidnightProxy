package org.wallentines.mdproxy.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdproxy.Proxy;
import org.wallentines.mdproxy.plugin.Plugin;
import org.wallentines.mdproxy.requirement.ConnectionCheckType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JWTPlugin implements Plugin {
    public static final Logger LOGGER = LoggerFactory.getLogger("JWTPlugin");
    private static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("key_store_path", "keystore");

    private KeyStore keyStore;

    @Override
    public void initialize(Proxy proxy) {

        Path configFolder = proxy.getPluginManager().configFolder().resolve("jwt");
        try { Files.createDirectories(configFolder); } catch (IOException e) {
            throw new RuntimeException("Could not create lang directory", e);
        }

        FileWrapper<ConfigObject> config = proxy.fileCodecRegistry().findOrCreate(ConfigContext.INSTANCE, "config", configFolder, DEFAULT_CONFIG);

        Path keyStoreDir = configFolder.resolve(config.getRoot().asSection().getString("key_store_path"));
        try { Files.createDirectories(keyStoreDir); } catch (IOException e) {
            throw new RuntimeException("Could not create key store directory", e);
        }
        keyStore = new FileKeyStore(keyStoreDir, FileKeyStore.DEFAULT_TYPES);

        proxy.getCommands().register("jwt", new JWTCommand());
        ConnectionCheckType.REGISTRY.tryRegister("jwt", JWTCheck.TYPE);
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }
}

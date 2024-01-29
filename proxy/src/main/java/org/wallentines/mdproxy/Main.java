package org.wallentines.mdproxy;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.lang.LangManager;
import org.wallentines.mcore.lang.LangRegistry;
import org.wallentines.mcore.lang.PlaceholderManager;
import org.wallentines.mdcfg.ConfigList;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileCodecRegistry;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger("Main");


    private static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("port", 25565)
            .with("haproxy_protocol", false)
            .with("backends", new ConfigList())
            .with("status", new ConfigList())
            .with("online_mode", true)
            .with("reconnect_threads", 4)
            .with("auth_threads", 4)
            .with("reconnect_timeout", 3000)
            .with("backend_timeout", 5000)
            .with("client_timeout", 15000)
            .with("player_limit", 100);


    public static void main(String[] args) {

        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

        FileCodecRegistry reg = new FileCodecRegistry();
        reg.registerFileCodec(JSONCodec.fileCodec());

        File configFile = new File("config.json");
        FileWrapper<ConfigObject> config = new FileWrapper<>(ConfigContext.INSTANCE, JSONCodec.fileCodec(), configFile, StandardCharsets.UTF_8, DEFAULT_CONFIG);

        File langDir = new File("lang");
        if(!langDir.isDirectory() && !langDir.mkdirs()) {
            throw new IllegalStateException("Unable to create lang directory!");
        }

        LangRegistry defaults;
        try {
            defaults = LangRegistry.fromConfig(
                    JSONCodec.loadConfig(Main.class.getClassLoader().getResourceAsStream("lang/en_us.json")).asSection(),
                    PlaceholderManager.INSTANCE
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load lang defaults!");
        }

        LangManager manager = new LangManager(defaults, langDir, reg, PlaceholderManager.INSTANCE);
        manager.saveLanguageDefaults("en_us", defaults);

        if(configFile.isFile()) {
            config.load();
        }

        config.save();

        ProxyServer ps = new ProxyServer(config, manager);

        try {
            ps.start();
        } catch (Exception ex) {
            LOGGER.error("An exception occurred while running the server!", ex);
            ps.shutdown();
        }
    }

}

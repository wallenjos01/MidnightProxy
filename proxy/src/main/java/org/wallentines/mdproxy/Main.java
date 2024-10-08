package org.wallentines.mdproxy;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.MidnightCoreAPI;
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
import org.wallentines.mdproxy.plugin.PluginLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger("Main");


    private static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("port", 25565)
            .with("haproxy_protocol", false)
            .with("backends", new ConfigSection())
            .with("status", new ConfigList())
            .with("routes", new ConfigList())
            .with("online_mode", true)
            .with("force_authentication", false)
            .with("auth_threads", 4)
            .with("reconnect_timeout_sec", 3)
            .with("backend_timeout_ms", 5000)
            .with("client_timeout_ms", 15000)
            .with("player_limit", 100)
            .with("icon_cache_dir", "icons")
            .with("icon_cache_size", 8)
            .with("prevent_proxy_connections", false)
            .with("log_status_messages", false)
            .with("reply_to_legacy_ping", true);


    public static void main(String[] args) {

        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

        MidnightCoreAPI.GLOBAL_CONFIG_DIRECTORY.set(Path.of("config"));

        ClientConnection.registerPlaceholders(PlaceholderManager.INSTANCE);
        Proxy.registerPlaceholders(PlaceholderManager.INSTANCE);

        FileCodecRegistry reg = MidnightCoreAPI.FILE_CODEC_REGISTRY;
        reg.registerFileCodec(JSONCodec.fileCodec());

        Path configFile = Paths.get("config.json");
        FileWrapper<ConfigObject> config = new FileWrapper<>(ConfigContext.INSTANCE, JSONCodec.fileCodec(), configFile, StandardCharsets.UTF_8, DEFAULT_CONFIG);

        Path currentDir = Paths.get(System.getProperty("user.dir"));
        if(!Files.isWritable(currentDir)) {
            throw new IllegalStateException("Working directory is not writable");
        }

        Path langDir = Paths.get("lang");
        if(!Files.isDirectory(langDir)) {
            try {
                Files.createDirectories(langDir);
            } catch (IOException e) {
                throw new RuntimeException("Could not create lang directory", e);
            }
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

        LangManager.registerPlaceholders(PlaceholderManager.INSTANCE);
        LangManager manager = new LangManager(defaults, langDir, reg, PlaceholderManager.INSTANCE);
        manager.saveLanguageDefaults("en_us", defaults);


        Path pluginDir = Paths.get("plugins");
        if(!Files.isDirectory(pluginDir)) {
            try {
                Files.createDirectories(pluginDir);
            } catch (IOException e) {
                throw new RuntimeException("Could not create lang directory", e);
            }
        }
        PluginLoader loader = new PluginLoader(pluginDir);

        ProxyServer ps = new ProxyServer(config, manager, loader);
        try {
            ps.start();
        } catch (Exception ex) {
            LOGGER.error("An error occurred while starting the proxy server!", ex);
        }

        Runtime.getRuntime().addShutdownHook(new Thread("Proxy Shutdown Thread") {
            @Override
            public void run() {
                ps.shutdown();
            }
        });

        ps.getConnectionManager().getBossGroup().terminationFuture().awaitUninterruptibly();
    }

}

package org.wallentines.mdproxy;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.ConfigList;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileCodecRegistry;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdproxy.plugin.PluginManagerImpl;
import org.wallentines.mdproxy.util.MessageUtil;
import org.wallentines.pseudonym.PartialMessage;
import org.wallentines.pseudonym.lang.LangManager;
import org.wallentines.pseudonym.lang.LangProvider;
import org.wallentines.pseudonym.lang.LangRegistry;
import org.wallentines.pseudonym.text.Component;
import org.wallentines.pseudonym.text.TextUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger("Main");


    private static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("port", 25565)
            .with("haproxy_protocol", false)
            .with("backends", new ConfigSection())
            .with("status", new ConfigList())
            .with("routes", new ConfigList())
            .with("auth_routes", new ConfigList().append(new ConfigSection().with("type", Authenticator.MOJANG_ID)))
            .with("auth_threads", 4)
            .with("auth_timeout_ms", 5000)
            .with("use_authentication", true)
            .with("force_authentication", false)
            .with("reconnect_timeout_sec", 3)
            .with("backend_timeout_ms", 5000)
            .with("client_timeout_ms", 15000)
            .with("player_limit", 100)
            .with("icon_cache_dir", "icons")
            .with("icon_cache_size", 8)
            .with("prevent_proxy_connections", false)
            .with("log_status_messages", false)
            .with("reply_to_legacy_ping", true)
            .with("boss_threads", 1)
            .with("worker_threads", 4);


    public static void main(String[] args) {

        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

        ClientConnection.registerPlaceholders(MessageUtil.PLACEHOLDERS);
        Proxy.registerPlaceholders(MessageUtil.PLACEHOLDERS);

        FileCodecRegistry reg = new FileCodecRegistry();
        reg.registerFileCodec(JSONCodec.fileCodec());

        Path workDir = Paths.get(System.getProperty("user.dir"));
        FileWrapper<ConfigObject> config = reg.findOrCreate(ConfigContext.INSTANCE, "config", workDir, StandardCharsets.UTF_8, DEFAULT_CONFIG);

        Path currentDir = Paths.get(System.getProperty("user.dir"));
        if(!Files.isWritable(currentDir)) {
            throw new IllegalStateException("Working directory is not writable");
        }

        Path langDir = Paths.get("lang");
        try { Files.createDirectories(langDir); } catch (IOException e) {
            throw new RuntimeException("Could not create lang directory", e);
        }


        LangRegistry<PartialMessage<String>> defaults;
        try {
            ConfigSection sec = JSONCodec.loadConfig(Main.class.getClassLoader().getResourceAsStream("lang/en_us.json")).asSection();
            Map<String, PartialMessage<String>> messages = new HashMap<>();
            for(String key : sec.getKeys()) {
                messages.put(key, MessageUtil.PARSE_PIPELINE.accept(key));
            }

            FileWrapper<ConfigObject> saved = reg.findOrCreate(ConfigContext.INSTANCE, "en_us", langDir, StandardCharsets.UTF_8, sec);
            saved.load();
            saved.save();

            defaults = new LangRegistry<>(messages);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load lang defaults!");
        }

        LangManager.registerPlaceholders(MessageUtil.PLACEHOLDERS);
        LangManager<PartialMessage<String>, Component> manager = new LangManager<>(Component.class, defaults, LangProvider.forDirectory(langDir, reg, MessageUtil.PARSE_PIPELINE), TextUtil.COMPONENT_RESOLVER);

        Authenticator.REGISTRY.register(Authenticator.MOJANG_ID, MojangAuthenticator.TYPE);

        Path pluginDir = Paths.get("plugins");
        try { Files.createDirectories(pluginDir); } catch (IOException e) {
            throw new RuntimeException("Could not create lang directory", e);
        }
        PluginManagerImpl loader = new PluginManagerImpl(pluginDir, Path.of("config"));

        ProxyServer ps = new ProxyServer(config, reg, manager, loader);
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

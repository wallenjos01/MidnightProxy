package org.wallentines.mdproxy.smi;

import org.wallentines.mcore.MidnightCoreAPI;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdproxy.Proxy;
import org.wallentines.mdproxy.plugin.Plugin;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.smi.Messenger;
import org.wallentines.smi.MessengerType;
import org.wallentines.smi.AmqpMessenger;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MessengerPlugin implements Plugin {

    private static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("messengers", new ConfigSection()
                    .with("default", new ConfigSection()
                            .with("type", "amqp")
                    )
            );

    private Map<String, Messenger> messengers = new HashMap<>();

    @Override
    public void initialize(Proxy proxy) {

        Path configFolder = MidnightCoreAPI.GLOBAL_CONFIG_DIRECTORY.get().resolve("whitelist");
        try { Files.createDirectories(configFolder); } catch (IOException e) {
            throw new RuntimeException("Could not create whitelist directory", e);
        }

        FileWrapper<ConfigObject> config = MidnightCoreAPI.FILE_CODEC_REGISTRY.findOrCreate(ConfigContext.INSTANCE, "config", configFolder, DEFAULT_CONFIG);
        messengers = Messenger.createSerializer(REGISTRY).mapOf().deserialize(ConfigContext.INSTANCE, config.getRoot().asSection().getSection("messengers")).getOrThrow();
    }

    public Messenger getMessenger(String name) {
        return messengers.get(name);
    }

    private static final Registry<Identifier, MessengerType<?>> REGISTRY = Registry.create("smi");

    static {
        AmqpMessenger.register(REGISTRY);
    }

}

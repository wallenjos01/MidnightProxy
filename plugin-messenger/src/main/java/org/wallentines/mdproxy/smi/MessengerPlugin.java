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
import org.wallentines.smi.MessengerManager;
import org.wallentines.smi.MessengerManagerImpl;
import org.wallentines.smi.AmqpMessenger;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MessengerPlugin implements Plugin {

    private static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("messengers", new ConfigSection()
                    .with("default", new ConfigSection()
                            .with("type", "amqp")
                    )
            );

    private MessengerManagerImpl manager;

    @Override
    public void initialize(Proxy proxy) {

        Path configFolder = MidnightCoreAPI.GLOBAL_CONFIG_DIRECTORY.get().resolve("messenger");
        try { Files.createDirectories(configFolder); } catch (IOException e) {
            throw new RuntimeException("Could not create messenger directory", e);
        }

        FileWrapper<ConfigObject> config = MidnightCoreAPI.FILE_CODEC_REGISTRY.findOrCreate(ConfigContext.INSTANCE, "config", configFolder, DEFAULT_CONFIG);
        manager = new MessengerManagerImpl(REGISTRY);
        manager.loadAll(config.getRoot().asSection().getSection("messengers"));

        if(MessengerManager.Holder.gInstance == null) {
            MessengerManagerImpl.register(manager);
        }

        proxy.shutdownEvent().register(this, prx -> {

            if(MessengerManager.Holder.gInstance == manager) {
                MessengerManager.Holder.gInstance = null;
            }
        });

    }

    public Messenger getMessenger(String name) {
        return manager.messenger(name);
    }

    private static final Registry<Identifier, MessengerType<?>> REGISTRY = Registry.create("smi");

    static {
        AmqpMessenger.register(REGISTRY);
    }

}

package org.wallentines.mdproxy.resources;

import org.wallentines.mcore.MidnightCoreAPI;
import org.wallentines.mdcfg.ConfigList;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdproxy.Proxy;
import org.wallentines.mdproxy.plugin.Plugin;
import org.wallentines.midnightlib.registry.Registry;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ResourcePackPlugin implements Plugin {

    private static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("packs", new ConfigSection())
            .with("routes", new ConfigSection())
            .with("global", new ConfigList());

    private final FileWrapper<ConfigObject> config;

    private Map<String, List<ResourcePack>> routePacks;
    private List<ResourcePack> globalPacks;

    public ResourcePackPlugin() {

        Path configFolder = MidnightCoreAPI.GLOBAL_CONFIG_DIRECTORY.get().resolve("resource_pack");
        try { Files.createDirectories(configFolder); } catch (IOException e) {
            throw new RuntimeException("Could not create messenger directory", e);
        }

        this.config = MidnightCoreAPI.FILE_CODEC_REGISTRY.findOrCreate(ConfigContext.INSTANCE, "config", configFolder, DEFAULT_CONFIG);
        config.save();

        reload();
    }

    private void reload() {

        config.load();
        ConfigSection root = config.getRoot().asSection();

        Registry<String, ResourcePack> packs = Registry.createStringRegistry();
        ResourcePack.SERIALIZER.mapOf().deserialize(ConfigContext.INSTANCE, root.getSection("packs")).getOrThrow().forEach(packs::register);

        routePacks = packs.byIdSerializer().listOf().mapToList().mapOf().deserialize(ConfigContext.INSTANCE, root.getSection("routes")).getOrThrow();
        globalPacks = packs.byIdSerializer().listOf().mapToList().deserialize(ConfigContext.INSTANCE, root.getList("global")).getOrThrow();
    }

    @Override
    public void initialize(Proxy proxy) {

        proxy.getCommands().register("rp", (sender, args) -> {
            if(args.length == 1 && args[0].equals("reload")) {
                reload();
                sender.sendMessage("Reloaded resource packs");
            }
        });

        proxy.clientConnectEvent().register(this, client -> {
            client.preConnectBackendEvent().register(this, ev -> {

                for(ResourcePack pack : globalPacks) {
                    ev.p2.send(pack.toPacket());
                }

                String id = proxy.getBackends().getId(ev.p1);
                if(id != null && routePacks.containsKey(id)) {
                    for(ResourcePack pack : routePacks.get(id)) {
                        ev.p2.send(pack.toPacket());
                    }
                }

            });
        });
    }

}

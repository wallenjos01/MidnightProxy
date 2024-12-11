package org.wallentines.mdproxy.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.MidnightCoreAPI;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdcfg.ConfigList;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdproxy.ClientConnection;
import org.wallentines.mdproxy.Proxy;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.common.ServerboundResourcePackStatusPacket;
import org.wallentines.mdproxy.plugin.Plugin;
import org.wallentines.midnightlib.registry.Registry;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ResourcePackPlugin implements Plugin {

    private static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("packs", new ConfigSection())
            .with("routes", new ConfigSection())
            .with("global", new ConfigList());
    private static final Logger log = LoggerFactory.getLogger(ResourcePackPlugin.class);
    private static final Component DEFAULT_KICK_MESSAGE = Component.translate("multiplayer.requiredTexturePrompt.disconnect");

    private final FileWrapper<ConfigObject> config;

    private Map<UUID, Component> kickMessages;
    private Map<String, List<ResourcePackEntry>> routePacks;
    private List<ResourcePackEntry> globalPacks;

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

        Registry<String, ResourcePackEntry> packs = Registry.createStringRegistry();
        ResourcePackEntry.SERIALIZER.mapOf().deserialize(ConfigContext.INSTANCE, root.getSection("packs")).getOrThrow().forEach(packs::register);

        kickMessages = new HashMap<>();
        for(ResourcePackEntry ent : packs.values()) {
            if(ent.required()) {
                Component kickMessage = ent.kickMessage();
                if(kickMessage == null) {
                    kickMessage = DEFAULT_KICK_MESSAGE;
                }
                kickMessages.put(ent.uuid(), kickMessage);
            }
        }

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

            if(client.getIntent() == ServerboundHandshakePacket.Intent.STATUS) return;
            client.preConnectBackendEvent().register(this, ev -> {

                if(ev.p2.wasReconnected()) {
                    return;
                }

                if(!ev.p2.authenticated()) {
                    log.info("Player {} is not authenticated!", ev.p2.username());
                    return;
                }

                List<CompletableFuture<?>> futures = new ArrayList<>();
                for(ResourcePackEntry pack : globalPacks) {
                    futures.add(ev.p2.sendResourcePack(pack.toPack()).thenAccept(pck -> onComplete(client, pck)));
                }

                String id = proxy.getBackends().getId(ev.p1);
                if(id != null && routePacks.containsKey(id)) {
                    for(ResourcePackEntry pack : routePacks.get(id)) {
                        futures.add(ev.p2.sendResourcePack(pack.toPack()).thenAccept(pck -> onComplete(client, pck)));
                    }
                }

                CompletableFuture.allOf(futures.toArray(CompletableFuture<?>[]::new)).join();
            });
        });
    }

    private void onComplete(ClientConnection conn, ServerboundResourcePackStatusPacket packet) {
        if(packet.action() == ServerboundResourcePackStatusPacket.Action.DECLINED && kickMessages.containsKey(packet.packId())) {
            conn.disconnect(kickMessages.get(packet.packId()));
        }
    }

}

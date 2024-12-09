package org.wallentines.mdproxy.resources;

import org.jetbrains.annotations.Nullable;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.text.ConfigSerializer;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.packet.config.ClientboundAddResourcePackPacket;

import java.util.UUID;

public record ResourcePack(UUID uuid, String url, @Nullable String hash, boolean required, @Nullable Component message) {

    public static final Serializer<ResourcePack> SERIALIZER = ObjectSerializer.create(
            Serializer.UUID.entry("uuid", ResourcePack::uuid),
            Serializer.STRING.entry("url", ResourcePack::url),
            Serializer.STRING.entry("hash", ResourcePack::hash).optional(),
            Serializer.BOOLEAN.entry("required", ResourcePack::required).orElse(false),
            ConfigSerializer.INSTANCE.entry("message", ResourcePack::message).optional(),
            ResourcePack::new
    );

    public ClientboundAddResourcePackPacket toPacket() {
        return new ClientboundAddResourcePackPacket(uuid, url, hash, required, message);
    }

}

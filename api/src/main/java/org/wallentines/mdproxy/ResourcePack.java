package org.wallentines.mdproxy;

import org.jetbrains.annotations.Nullable;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdproxy.packet.common.ClientboundAddResourcePackPacket;

import java.util.UUID;

public record ResourcePack(UUID uuid, String url, @Nullable String hash, boolean required, @Nullable Component message) {

    public ClientboundAddResourcePackPacket toPacket() {
        return new ClientboundAddResourcePackPacket(uuid, url, hash, required, message);
    }

}

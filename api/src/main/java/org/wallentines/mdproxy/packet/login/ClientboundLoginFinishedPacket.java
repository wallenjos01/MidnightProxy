package org.wallentines.mdproxy.packet.login;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.Map;

public record ClientboundLoginFinishedPacket(GameProfile gameProfile) implements Packet<ClientboundPacketHandler> {

    public static final PacketType<ClientboundPacketHandler> TYPE = PacketType.of(2, (ver, phase, buf) -> {
        throw new UnsupportedOperationException("Cannot deserialize clientbound packet!");
    });

    @Override
    public PacketType<ClientboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void handle(ClientboundPacketHandler handler) {
        throw new UnsupportedOperationException("Unable to handle clientbound packet");
    }

    @Override
    public void write(GameVersion version, ProtocolPhase phase, ByteBuf buf) {

        PacketBufferUtil.writeUUID(buf, gameProfile.getId());
        PacketBufferUtil.writeUtf(buf, gameProfile.getName(), 16);

        PropertyMap map = gameProfile.getProperties();
        PacketBufferUtil.writeVarInt(buf, map.size());
        for(Map.Entry<String, Property> ent : map.entries()) {

            PacketBufferUtil.writeUtf(buf, ent.getKey());
            PacketBufferUtil.writeUtf(buf, ent.getValue().value());
            PacketBufferUtil.writeOptional(buf, ent.getValue().signature(), PacketBufferUtil::writeUtf);
        }
    }
}

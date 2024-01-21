package org.wallentines.mdproxy.packet;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.Map;

public record ClientboundLoginFinishedPacket(GameProfile gameProfile) implements Packet {

    public static final int ID = 2;

    @Override
    public int getId() {
        return ID;
    }

    @Override
    public void write(ByteBuf buf) {

        PacketBufferUtil.writeUUID(buf, gameProfile.getId());
        PacketBufferUtil.writeUtf(buf, gameProfile.toString(), 16);

        PropertyMap map = gameProfile.getProperties();
        PacketBufferUtil.writeVarInt(buf, map.size());
        for(Map.Entry<String, Property> ent : map.entries()) {

            PacketBufferUtil.writeUtf(buf, ent.getKey());
            PacketBufferUtil.writeUtf(buf, ent.getValue().value());
            PacketBufferUtil.writeOptional(buf, ent.getValue().signature(), PacketBufferUtil::writeUtf);
        }
    }
}

package org.wallentines.mdproxy.packet.login;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.util.PacketBufferUtil;

public record ClientboundKickPacket(Component message) implements Packet {

    public static final PacketType TYPE = PacketType.of(0, buf -> {
        throw new UnsupportedOperationException("Cannot deserialize clientbound packet!");
    });

    @Override
    public PacketType getType() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketBufferUtil.writeUtf(buf, message.toJSONString());
    }

}

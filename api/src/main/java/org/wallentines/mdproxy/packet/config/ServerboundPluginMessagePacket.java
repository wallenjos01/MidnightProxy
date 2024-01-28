package org.wallentines.mdproxy.packet.config;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;

public class ServerboundPluginMessagePacket implements Packet {

    public static final PacketType TYPE = PacketType.of(2, buf -> new ServerboundSettingsPacket());
    @Override
    public PacketType getType() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) { }
}

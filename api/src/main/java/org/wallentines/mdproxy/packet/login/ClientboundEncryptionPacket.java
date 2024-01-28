package org.wallentines.mdproxy.packet.login;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.util.PacketBufferUtil;

public record ClientboundEncryptionPacket(String serverId, byte[] publicKey, byte[] verifyToken, boolean authEnabled) implements Packet {


    public static final PacketType TYPE = PacketType.of(1, buf -> {
        throw new UnsupportedOperationException("Cannot deserialize clientbound packet!");
    });

    @Override
    public PacketType getType() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketBufferUtil.writeUtf(buf, serverId);
        PacketBufferUtil.writeVarInt(buf, publicKey.length);
        buf.writeBytes(publicKey);
        PacketBufferUtil.writeVarInt(buf, verifyToken.length);
        buf.writeBytes(verifyToken);
        buf.writeBoolean(authEnabled);
    }
}

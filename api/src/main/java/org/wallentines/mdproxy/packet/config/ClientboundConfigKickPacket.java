package org.wallentines.mdproxy.packet.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.EncoderException;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.text.ModernSerializer;
import org.wallentines.mdcfg.codec.EncodeException;
import org.wallentines.mdcfg.codec.NBTCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;

import java.io.IOException;

public record ClientboundConfigKickPacket(Component message) implements Packet<ClientboundPacketHandler> {

    public static final PacketType<ClientboundPacketHandler> TYPE = PacketType.of(ver -> ver.hasFeature(GameVersion.Feature.TRANSFER_PACKETS) ? 2 : 1, (ver, buf) -> {
        throw new UnsupportedOperationException("Cannot deserialize clientbound packet!");
    });

    @Override
    public PacketType<ClientboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(GameVersion version, ByteBuf buf) {

        ByteBufOutputStream bos = new ByteBufOutputStream(buf);
        try {
            new NBTCodec(false).encode(ConfigContext.INSTANCE, ModernSerializer.INSTANCE.forContext(GameVersion.MAX), message, bos);
        } catch (IOException | EncodeException ex) {
            throw new EncoderException("Unable to encode NBT kick packet!", ex);
        }
    }

    @Override
    public void handle(ClientboundPacketHandler handler) {
        throw new UnsupportedOperationException("Unable to handle clientbound packet");
    }


}

package org.wallentines.mdproxy.packet.login;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;
import org.wallentines.mdproxy.util.CryptUtil;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import javax.crypto.SecretKey;
import java.security.PublicKey;

public record ServerboundEncryptionPacket(byte[] sharedSecret, byte[] verifyToken) implements Packet<ServerboundPacketHandler> {

    public ServerboundEncryptionPacket(SecretKey key, PublicKey publicKey, byte[] verifyBytes) {
        this(CryptUtil.encryptData(publicKey, key.getEncoded()), CryptUtil.encryptData(publicKey, verifyBytes));
    }

    public static final PacketType<ServerboundPacketHandler> TYPE = PacketType.of(1, ServerboundEncryptionPacket::read);

    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }


    @Override
    public void write(GameVersion version, ByteBuf buf) {
        PacketBufferUtil.writeVarInt(buf, sharedSecret.length);
        buf.writeBytes(sharedSecret);
        PacketBufferUtil.writeVarInt(buf, verifyToken.length);
        buf.writeBytes(verifyToken);
    }
    @Override
    public void handle(ServerboundPacketHandler handler) {
        handler.handle(this);
    }

    public static ServerboundEncryptionPacket read(GameVersion ver, ByteBuf buf) {

        byte[] secret = new byte[PacketBufferUtil.readVarInt(buf)];
        buf.readBytes(secret);

        byte[] token = new byte[PacketBufferUtil.readVarInt(buf)];
        buf.readBytes(token);

        return new ServerboundEncryptionPacket(secret, token);
    }
}

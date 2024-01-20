package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.util.CryptUtil;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import javax.crypto.SecretKey;
import java.security.PublicKey;

public record ServerboundEncryptionPacket(byte[] sharedSecret, byte[] verifyToken) implements Packet {

    public static final int ID = 1;

    public ServerboundEncryptionPacket(SecretKey key, PublicKey publicKey, byte[] verifyBytes) {
        this(CryptUtil.encryptData(publicKey, key.getEncoded()), CryptUtil.encryptData(publicKey, verifyBytes));
    }

    @Override
    public int getId() {
        return ID;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketBufferUtil.writeVarInt(buf, sharedSecret.length);
        buf.writeBytes(sharedSecret);
        PacketBufferUtil.writeVarInt(buf, verifyToken.length);
        buf.writeBytes(verifyToken);
    }

    public static ServerboundEncryptionPacket read(ByteBuf buf) {

        byte[] secret = new byte[PacketBufferUtil.readVarInt(buf)];
        buf.readBytes(secret);

        byte[] token = new byte[PacketBufferUtil.readVarInt(buf)];
        buf.readBytes(token);

        return new ServerboundEncryptionPacket(secret, token);
    }
}

package org.wallentines.mdproxy.packet;

import org.wallentines.mdproxy.packet.common.ServerboundCookiePacket;
import org.wallentines.mdproxy.packet.config.ServerboundPluginMessagePacket;
import org.wallentines.mdproxy.packet.config.ServerboundSettingsPacket;
import org.wallentines.mdproxy.packet.login.ServerboundEncryptionPacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginFinishedPacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginPacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginQueryPacket;
import org.wallentines.mdproxy.packet.status.ServerboundPingPacket;
import org.wallentines.mdproxy.packet.status.ServerboundStatusPacket;

public interface ServerboundPacketHandler {

    void handle(ServerboundHandshakePacket handshake);

    void handle(ServerboundStatusPacket ping);

    void handle(ServerboundPingPacket ping);

    void handle(ServerboundLoginPacket login);

    void handle(ServerboundLoginQueryPacket message);

    void handle(ServerboundEncryptionPacket encrypt);

    void handle(ServerboundLoginFinishedPacket finished);

    void handle(ServerboundCookiePacket cookie);

    void handle(ServerboundPluginMessagePacket message);

    void handle(ServerboundSettingsPacket settings);


}

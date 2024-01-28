package org.wallentines.mdproxy;

import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginPacket;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.UUID;

public interface ClientConnection {

    boolean authenticated();
    boolean cookiesAvailable();
    boolean canTransfer();

    String hostname();

    int port();

    String username();

    UUID uuid();

    byte[] getCookie(Identifier id);

    ServerboundHandshakePacket handshakePacket(ServerboundHandshakePacket.Intent intent);

    ServerboundLoginPacket loginPacket();

}

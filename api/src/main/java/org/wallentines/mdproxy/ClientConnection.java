package org.wallentines.mdproxy;

import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginPacket;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.UUID;

public interface ClientConnection {

    boolean playerInfoAvailable();
    boolean authenticated();
    boolean cookiesAvailable();
    boolean canTransfer();
    boolean localeAvailable();

    TestResult bypassesPlayerLimit(Proxy proxy);

    String hostname();

    int port();
    int protocolVersion();

    String username();

    UUID uuid();

    PlayerInfo playerInfo();

    byte[] getCookie(Identifier id);

    String locale();

    ServerboundHandshakePacket handshakePacket(ServerboundHandshakePacket.Intent intent);

    ServerboundLoginPacket loginPacket();

}

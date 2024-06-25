package org.wallentines.mdproxy;

import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.login.ClientboundLoginQueryPacket;
import org.wallentines.mdproxy.packet.status.ClientboundPingPacket;
import org.wallentines.mdproxy.packet.status.ClientboundStatusPacket;
import org.wallentines.mdproxy.packet.status.ServerboundPingPacket;
import org.wallentines.mdproxy.packet.status.ServerboundStatusPacket;

public class StatusResponder implements ClientboundPacketHandler {

    private static final Component HANDLED_MESSAGE = Component.translate("multiplayer.status.request_handled");

    private final ClientConnectionImpl conn;
    private final ProxyServer server;
    private final StatusEntry entry;
    private BackendConnectionImpl backend;
    private boolean responded = false;

    public StatusResponder(ClientConnectionImpl conn, ProxyServer server, StatusEntry entry) {
        this.conn = conn;
        this.server = server;
        this.entry = entry;
    }

    public void status(GameVersion playerVersion) {

        if(entry.shouldPassthrough()) {

            String backendName = entry.passthrough();
            Backend b = server.getBackends().get(backendName);

            if(b == null) {
                throw new IllegalStateException("Unable to find backend " + backendName + "!");
            }

            server.getConnectionManager().connectToBackend(conn, b, playerVersion, server.getBackendTimeout())
                    .thenAccept(backend -> {
                        backend.setupStatus(this);
                        backend.send(conn.handshakePacket(ServerboundHandshakePacket.Intent.STATUS));
                        backend.changePhase(ProtocolPhase.STATUS);
                        backend.send(new ServerboundStatusPacket());
                        this.backend = backend;
                    });

            return;
        }

        StatusMessage message = entry.create(playerVersion, server.getOnlinePlayers(), server.getPlayerLimit(), server.getIconCache());
        conn.send(new ClientboundStatusPacket(message));
    }

    public void ping(ServerboundPingPacket pck) {

        if(backend != null) {

            backend.send(pck);
            return;
        }

        conn.send(new ClientboundPingPacket(pck.value()));

    }

    @Override
    public void handle(ClientboundStatusPacket status) {

        if(responded) {
            conn.disconnect(HANDLED_MESSAGE, false);
            return;
        }
        conn.send(new ClientboundStatusPacket(entry.resolve(status.data(), server.getIconCache())));
        responded = true;
    }

    @Override
    public void handle(ClientboundLoginQueryPacket message) {

    }

    @Override
    public void handle(ClientboundPingPacket ping) {

        conn.send(new ClientboundPingPacket(ping.value()));
        conn.disconnect(HANDLED_MESSAGE, false);
    }

}

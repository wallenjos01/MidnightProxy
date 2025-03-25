package org.wallentines.mdproxy;

import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.login.ClientboundLoginQueryPacket;
import org.wallentines.mdproxy.packet.status.ClientboundPingPacket;
import org.wallentines.mdproxy.packet.status.ClientboundStatusPacket;
import org.wallentines.mdproxy.packet.status.ServerboundPingPacket;
import org.wallentines.mdproxy.packet.status.ServerboundStatusPacket;
import org.wallentines.pseudonym.text.Component;
import org.wallentines.pseudonym.text.Content;
import org.wallentines.pseudonym.text.ImmutableComponent;
import org.wallentines.pseudonym.text.Style;

import java.util.Collections;

public class StatusResponder implements ClientboundPacketHandler {

    private static final Component HANDLED_MESSAGE = new ImmutableComponent(new Content.Translate("multiplayer.status.request_handled"), Style.EMPTY, Collections.emptyList());

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

            server.getConnectionManager().connectToBackend(conn, b, playerVersion.protocolVersion(), server.getBackendTimeout())
                    .thenAccept(backend -> {
                        backend.setupStatus(this);
                        backend.send(new ServerboundHandshakePacket(conn.protocolVersion(), conn.hostname(), conn.port(), ServerboundHandshakePacket.Intent.STATUS));
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
        conn.disconnect();
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

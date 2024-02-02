package org.wallentines.mdproxy;

import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.status.ClientboundPingPacket;
import org.wallentines.mdproxy.packet.status.ClientboundStatusPacket;
import org.wallentines.mdproxy.packet.status.ServerboundPingPacket;
import org.wallentines.mdproxy.packet.status.ServerboundStatusPacket;

public class StatusResponder implements ClientboundPacketHandler {

    private final ClientConnectionImpl conn;
    private final ProxyServer server;
    private final StatusEntry entry;
    private BackendConnectionImpl backend;

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

            backend = new BackendConnectionImpl(conn, b, playerVersion, server.getBackendTimeout());
            backend.connect(conn.getChannel().eventLoop()).addListener(future -> {
                if(future.isSuccess()) {
                    backend.setupStatus(this);
                    backend.send(conn.handshakePacket(ServerboundHandshakePacket.Intent.STATUS));
                    backend.changePhase(ProtocolPhase.STATUS);
                    backend.send(new ServerboundStatusPacket());
                }
            });
            return;
        }

        StatusMessage message = entry.create(playerVersion, server.getOnlinePlayers(), server.getPlayerLimit());
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

        conn.send(new ClientboundStatusPacket(entry.resolve(status.data(), server.getIconCache())));
    }

    @Override
    public void handle(ClientboundPingPacket ping) {

        conn.send(new ClientboundPingPacket(ping.value()));
    }

}

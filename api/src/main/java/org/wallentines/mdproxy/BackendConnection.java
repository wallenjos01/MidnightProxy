package org.wallentines.mdproxy;

import org.jetbrains.annotations.Nullable;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;

/**
 * Represents a connection to a backend server.
 */
public interface BackendConnection {

    /**
     * Gets the backend corresponding to this connection.
     * @return The backend server.
     */
    Backend getBackend();

    /**
     * Determines whether the proxy is connected to the backend server.
     * @return Whether the proxy is connected to the backend.
     */
    boolean isConnected();

    /**
     * Determines whether the proxy is forwarding packets between the client and this backend server.
     * @return Whether the proxy is forwarding packets for this connection.
     */
    boolean isForwarding();

    /**
     * Gets the ID of the backend server, according to the given proxy.
     * @param proxy The proxy to check.
     * @return This backend's ID, or null.
     */
    @Nullable
    String getBackendId(Proxy proxy);

    /**
     * Determines if this backend is ephemeral (unregistered).
     * @param proxy The proxy to check.
     * @return Whether the backend is ephemeral.
     */
    default boolean isEphemeral(Proxy proxy) {
        return getBackendId(proxy) == null;
    }

    /**
     * Sends a packet to the backend server. Only applicable if the proxy is not forwarding packets for this connection.
     * @param packet The packet to send
     */
    void send(Packet<ServerboundPacketHandler> packet);

}

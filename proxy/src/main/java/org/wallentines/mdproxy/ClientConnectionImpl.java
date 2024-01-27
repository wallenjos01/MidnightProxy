package org.wallentines.mdproxy;

import org.wallentines.midnightlib.registry.Identifier;

import java.util.Map;
import java.util.UUID;

public class ClientConnectionImpl implements ClientConnection {

    private final String hostname;
    private final int port;
    private final String username;
    private final UUID uuid;
    private final boolean auth;
    private final boolean transferable;
    private final Map<Identifier, byte[]> cookies;

    public ClientConnectionImpl(String hostname, int port, String username, UUID uuid) {
        this(hostname, port, username, uuid, false, null, false);
    }

    private ClientConnectionImpl(String hostname, int port, String username, UUID uuid, boolean auth, Map<Identifier, byte[]> cookies, boolean transferable) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.uuid = uuid;
        this.auth = auth;
        this.cookies = cookies;
        this.transferable = transferable;
    }

    @Override
    public boolean authenticated() {
        return auth;
    }

    @Override
    public boolean cookiesAvailable() {
        return cookies != null;
    }

    @Override
    public boolean canTransfer() {
        return transferable;
    }

    @Override
    public String hostname() {
        return hostname;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public byte[] getCookie(Identifier id) {
        return cookies == null ? null : cookies.get(id);
    }

    public ClientConnectionImpl withAuth() {
        return new ClientConnectionImpl(hostname, port, username, uuid, true, cookies, transferable);
    }

    public ClientConnectionImpl withCookies(Map<Identifier, byte[]> cookies) {
        return new ClientConnectionImpl(hostname, port, username, uuid, auth, cookies, transferable);
    }

    public ClientConnectionImpl withTransferable() {
        return new ClientConnectionImpl(hostname, port, username, uuid, auth, cookies, true);
    }
}

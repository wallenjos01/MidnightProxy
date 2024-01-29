package org.wallentines.mdproxy;

import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginPacket;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.Map;
import java.util.UUID;

public class ClientConnectionImpl implements ClientConnection {

    private final int protocolVersion;
    private final String hostname;
    private final int port;
    private final String username;
    private final UUID uuid;
    private final boolean auth;
    private final boolean transferable;
    private final Map<Identifier, byte[]> cookies;
    private final String locale;

    public ClientConnectionImpl(int protocolVersion, String hostname, int port) {
        this(protocolVersion, hostname, port, null, null, false, null, false, null);
    }

    private ClientConnectionImpl(int protocolVersion, String hostname, int port, String username, UUID uuid, boolean auth, Map<Identifier, byte[]> cookies, boolean transferable, String locale) {
        this.protocolVersion = protocolVersion;
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.uuid = uuid;
        this.auth = auth;
        this.cookies = cookies;
        this.transferable = transferable;
        this.locale = locale;
    }

    @Override
    public boolean nameAvailable() {
        return username != null;
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
    public boolean localeAvailable() {
        return locale != null;
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

    @Override
    public String locale() {
        return locale;
    }

    @Override
    public ServerboundHandshakePacket handshakePacket(ServerboundHandshakePacket.Intent intent) {
        return new ServerboundHandshakePacket(protocolVersion, hostname, port, intent);
    }

    @Override
    public ServerboundLoginPacket loginPacket() {
        return new ServerboundLoginPacket(username, uuid);
    }

    public ClientConnectionImpl withName(String username, UUID uuid) {
        return new ClientConnectionImpl(protocolVersion, hostname, port, username, uuid, true, cookies, transferable, locale);
    }

    public ClientConnectionImpl withAuth() {
        return new ClientConnectionImpl(protocolVersion, hostname, port, username, uuid, true, cookies, transferable, locale);
    }

    public ClientConnectionImpl withCookies(Map<Identifier, byte[]> cookies) {
        return new ClientConnectionImpl(protocolVersion, hostname, port, username, uuid, auth, cookies, transferable, locale);
    }

    public ClientConnectionImpl withTransferable() {
        return new ClientConnectionImpl(protocolVersion, hostname, port, username, uuid, auth, cookies, true, locale);
    }

    public ClientConnectionImpl withLocale(String locale) {
        return new ClientConnectionImpl(protocolVersion, hostname, port, username, uuid, auth, cookies, transferable, locale);
    }
}

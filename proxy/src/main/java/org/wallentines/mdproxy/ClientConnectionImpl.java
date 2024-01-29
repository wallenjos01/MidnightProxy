package org.wallentines.mdproxy;

import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.lang.LocaleHolder;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginPacket;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.Map;
import java.util.UUID;

public class ClientConnectionImpl implements ClientConnection, LocaleHolder {

    private final int protocolVersion;
    private final String hostname;
    private final int port;
    private final PlayerInfo playerInfo;
    private final boolean auth;
    private final boolean transferable;
    private final Map<Identifier, byte[]> cookies;
    private final String locale;

    public ClientConnectionImpl(int protocolVersion, String hostname, int port) {
        this(protocolVersion, hostname, port, null, false, null, false, null);
    }

    private ClientConnectionImpl(int protocolVersion, String hostname, int port, PlayerInfo info, boolean auth, Map<Identifier, byte[]> cookies, boolean transferable, String locale) {
        this.protocolVersion = protocolVersion;
        this.hostname = hostname;
        this.port = port;
        this.playerInfo = info;
        this.auth = auth;
        this.cookies = cookies;
        this.transferable = transferable;
        this.locale = locale;
    }

    @Override
    public boolean playerInfoAvailable() {
        return playerInfo != null;
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
    public TestResult bypassesPlayerLimit(Proxy server) {

        if(server.getPlayerLimit() == -1) {
            return TestResult.PASS;
        }
        if(!authenticated()) {
            return TestResult.NOT_ENOUGH_INFO;
        }

        return TestResult.FAIL;
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
    public int protocolVersion() {
        return protocolVersion;
    }

    @Override
    public String username() {
        return playerInfo == null ? null : playerInfo.username();
    }

    @Override
    public UUID uuid() {
        return playerInfo == null ? null : playerInfo.uuid();
    }

    @Override
    public PlayerInfo playerInfo() {
        return playerInfo;
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
        return new ServerboundLoginPacket(playerInfo.username(), playerInfo.uuid());
    }

    public ClientConnectionImpl withPlayerInfo(PlayerInfo info) {
        return new ClientConnectionImpl(protocolVersion, hostname, port, info, true, cookies, transferable, locale);
    }

    public ClientConnectionImpl withAuth() {
        return new ClientConnectionImpl(protocolVersion, hostname, port, playerInfo, true, cookies, transferable, locale);
    }

    public ClientConnectionImpl withCookies(Map<Identifier, byte[]> cookies) {
        return new ClientConnectionImpl(protocolVersion, hostname, port, playerInfo, auth, cookies, transferable, locale);
    }

    public ClientConnectionImpl withTransferable() {
        return new ClientConnectionImpl(protocolVersion, hostname, port, playerInfo, auth, cookies, true, locale);
    }

    public ClientConnectionImpl withLocale(String locale) {
        return new ClientConnectionImpl(protocolVersion, hostname, port, playerInfo, auth, cookies, transferable, locale);
    }

    @Override
    public String getLanguage() {
        return locale == null ? "en_us" : locale;
    }
}

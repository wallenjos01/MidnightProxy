package org.wallentines.mdproxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.lang.LocaleHolder;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.text.ComponentResolver;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.config.ClientboundConfigKickPacket;
import org.wallentines.mdproxy.packet.login.ClientboundKickPacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginPacket;
import org.wallentines.midnightlib.registry.Identifier;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientConnectionImpl implements ClientConnection, LocaleHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger("ClientConnectionImpl");

    private final Channel channel;
    private final int protocolVersion;
    private final String hostname;
    private final int port;
    private PlayerInfo playerInfo;
    private boolean auth;
    private boolean cookiesAvailable;
    private boolean transferable;
    private String locale;
    private BackendConnectionImpl backend;
    private final Map<Identifier, byte[]> cookies = new HashMap<>();



    public ClientConnectionImpl(Channel channel, int protocolVersion, String hostname, int port) {
        this.channel = channel;
        this.protocolVersion = protocolVersion;
        this.hostname = hostname;
        this.port = port;
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
        return cookiesAvailable;
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
    public InetAddress address() {
        return ((InetSocketAddress) channel.remoteAddress()).getAddress();
    }

    @Override
    public TestResult bypassesPlayerLimit(Proxy server) {

        if(server.getPlayerLimit() == -1) {
            return TestResult.PASS;
        }
        if(!authenticated()) {
            return TestResult.NOT_ENOUGH_INFO;
        }

        return server.bypassesPlayerLimit(playerInfo) ? TestResult.PASS : TestResult.FAIL;
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
        return playerInfo == null ? channel.remoteAddress().toString() : playerInfo.username();
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
        return cookies.get(id);
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

    @Override
    public boolean isConnected() {
        return backend != null;
    }

    @Override
    public BackendConnection getBackendConnection() {
        return backend;
    }

    public void setPlayerInfo(PlayerInfo playerInfo) {
        this.playerInfo = playerInfo;
    }

    public void setAuthenticated(boolean auth) {
        this.auth = auth;
    }

    public void setTransferable(boolean transferable) {
        this.transferable = transferable;
    }

    public void setCookiesAvailable() {
        this.cookiesAvailable = true;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setCookie(Identifier id, byte[] data) {
        this.cookies.put(id, data);
    }

    public void setBackend(BackendConnectionImpl backend) {
        this.backend = backend;
    }

    public void send(Packet<ClientboundPacketHandler> packet) {

        send(packet, null);
    }

    public void send(Packet<ClientboundPacketHandler> packet, ChannelFutureListener listener) {

        if(backend != null) {
            LOGGER.warn("Attempt to send packet to player {} after they connected to a backend!", username());
            return;
        }

        if(channel.eventLoop().inEventLoop()) {
            doSend(packet, listener);
        } else {
            channel.eventLoop().execute(() -> doSend(packet, listener));
        }
    }

    private void doSend(Packet<ClientboundPacketHandler> packet, ChannelFutureListener listener) {

        try {
            ChannelFuture future = channel.writeAndFlush(packet);

            if (listener != null) {
                future.addListener(listener);
            }
        } catch (Exception ex) {
            LOGGER.error("An exception occurred while sending a packet!", ex);
            channel.close();
        }
    }

    public void disconnect(ProtocolPhase phase, Component component) {

        Component cmp = ComponentResolver.resolveComponent(component, this);

        LOGGER.info("Disconnecting player {}: {}", username(), cmp.allText());

        if(backend == null) {
            Packet<ClientboundPacketHandler> p = phase == ProtocolPhase.CONFIG ? new ClientboundConfigKickPacket(cmp) : new ClientboundKickPacket(cmp);
            channel.writeAndFlush(p).addListener(ChannelFutureListener.CLOSE);
            channel.config().setAutoRead(false);
        } else {
            channel.close();
        }
    }

    public void disconnect() {
        LOGGER.info("Disconnecting player {}", username());
        channel.close();
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public String getLanguage() {
        return locale == null ? "en_us" : locale;
    }
}

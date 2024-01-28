package org.wallentines.mdproxy;

import com.google.common.primitives.Ints;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdproxy.netty.*;
import org.wallentines.mdproxy.packet.*;
import org.wallentines.mdproxy.packet.config.*;
import org.wallentines.mdproxy.packet.login.*;
import org.wallentines.mdproxy.packet.status.ClientboundPingPacket;
import org.wallentines.mdproxy.packet.status.ClientboundStatusPacket;
import org.wallentines.mdproxy.packet.status.ServerboundPingPacket;
import org.wallentines.mdproxy.packet.status.ServerboundStatusPacket;
import org.wallentines.mdproxy.util.CryptUtil;
import org.wallentines.mdproxy.util.StringUtil;
import org.wallentines.midnightlib.registry.Identifier;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Random;

public class ClientPacketHandler implements ServerboundPacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("ClientPacketHandler");
    private static final Identifier RECONNECT_COOKIE = new Identifier("mdp", "rid");

    private final ProxyServer server;
    private final Channel channel;
    private byte[] challenge;

    private GameVersion version;
    private ServerboundHandshakePacket handshake;
    private ServerboundLoginPacket login;
    private ClientConnectionImpl conn;
    private boolean encrypted;
    private GameProfile profile;
    private final Random rand = new Random();
    private ProtocolPhase phase;

    private final PriorityQueue<Backend> backendQueue;
    private final HashSet<Identifier> requiredCookies = new HashSet<>();
    private final HashMap<Identifier, byte[]> cookies = new HashMap<>();


    public ClientPacketHandler(Channel channel, ProxyServer server) {

        this.version = GameVersion.MAX;
        this.server = server;
        this.channel = channel;
        this.encrypted = false;
        this.phase = ProtocolPhase.HANDSHAKE;

        this.backendQueue = new PriorityQueue<>(server.getBackends());
    }

    @Override
    public void handle(ServerboundHandshakePacket handshake) {

        LOGGER.info("Received handshake from " + getUsername() + " to " + handshake.address() + " (" + handshake.intent().name() + ")");
        this.handshake = handshake;

        if(handshake.intent() == ServerboundHandshakePacket.Intent.STATUS) {
            changePhase(ProtocolPhase.STATUS);
        } else {
            changePhase(ProtocolPhase.LOGIN);
        }
    }

    @Override
    public void handle(ServerboundStatusPacket ping) {

        send(new ClientboundStatusPacket(
                new GameVersion("MidnightProxy", handshake.protocolVersion()),
                Component.text("A MidnightProxy Server"),
                100, 0,
                false, false
        ));
    }

    @Override
    public void handle(ServerboundPingPacket ping) {

        send(new ClientboundPingPacket(ping.value()));
    }

    @Override
    public void handle(ServerboundLoginPacket login) {

        version = new GameVersion("", handshake.protocolVersion());

        if (backendQueue.isEmpty()) {
            disconnect(Component.text("There are no backend servers available!"));
            return;
        }

        this.login = login;
        this.conn = new ClientConnectionImpl(handshake.protocolVersion(), handshake.address(), handshake.port(), login.username(), login.uuid());

        if(tryConnectBackend()) {
            return;
        }

        if(!version.hasFeature(GameVersion.Feature.TRANSFER_PACKETS)) {
            disconnect(Component.text("This server requires at least version 1.20.5! (24w03a)"));
            return;
        }

        if(handshake.intent() == ServerboundHandshakePacket.Intent.TRANSFER) {

            send(new ClientboundCookieRequestPacket(RECONNECT_COOKIE));

        } else {
            this.profile = new GameProfile(login.uuid(), login.username());

            // Continue with login
            startLogin();
        }
    }

    @Override
    public void handle(ServerboundEncryptionPacket encrypt) {
        if(login == null || challenge == null) {
            throw new IllegalStateException("Received unrequested encryption packet!");
        }

        PrivateKey privateKey = server.getKeyPair().getPrivate();
        if(!MessageDigest.isEqual(challenge, CryptUtil.decryptData(privateKey, encrypt.verifyToken()))) {
            throw new IllegalStateException("Encryption challenge failed!");
        }

        byte[] decryptedSecret = CryptUtil.decryptData(privateKey, encrypt.sharedSecret());
        if (server.requiresAuth()) {

            LOGGER.info("Starting authentication for {}", login.username());

            String serverId = new BigInteger(CryptUtil.hashServerId(decryptedSecret, server.getKeyPair().getPublic())).toString(16);
            try {

                SocketAddress socketAddress = channel.remoteAddress();
                InetAddress addr = ((InetSocketAddress) socketAddress).getAddress();

                ProfileResult res = server.getSessionService().hasJoinedServer(login.username(), serverId, addr);
                if (res == null) {
                    disconnect(Component.translate("multiplayer.disconnect.unverified_username"));
                    return;
                }

                this.conn = conn.withAuth();
                profile = res.profile();

            } catch (AuthenticationUnavailableException ex) {
                disconnect(Component.translate("multiplayer.disconnect.authservers_down"));
                return;
            }
        } else {
            this.conn = conn.withAuth();
        }

        LOGGER.info("User {} signed in with UUID {}", getUsername(), profile.getId());

        try {
            setupEncryption(decryptedSecret);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Encryption unsuccessful!");
        }

        for (Backend b : backendQueue) {
            requiredCookies.addAll(b.getRequiredCookies());
        }

        if (requiredCookies.isEmpty()) {
            send(new ClientboundLoginFinishedPacket(profile));

        } else {
            for(Identifier i : requiredCookies) {
                send(new ClientboundCookieRequestPacket(i));
            }
        }
    }

    @Override
    public void handle(ServerboundLoginFinishedPacket finished) {

        changePhase(ProtocolPhase.CONFIG);
        conn = conn.withTransferable();
    }

    @Override
    public void handle(ServerboundCookiePacket cookie) {

        if(cookie.key().equals(RECONNECT_COOKIE)) {

            String id = cookie.data().map(bytes -> new String(cookie.data().orElseThrow(), StandardCharsets.US_ASCII)).orElse(null);
            ClientConnectionImpl newConn = id == null ? null : server.getReconnectCache().get(id);

            if(newConn == null) {
                startLogin();
                return;
            }

            if(!newConn.username().equals(login.username()) || !newConn.uuid().equals(login.uuid())) {
                disconnect(Component.text("Invalid reconnection!"));
                return;
            }

            server.getReconnectCache().clear(id);
            conn = newConn;

            LOGGER.info("User {} reconnected with UUID {}", getUsername(), login.uuid());

            if(!tryConnectBackend()) {
                disconnect(Component.text("Unable to find backend server!"));
            }

            return;
        }


        if(requiredCookies.remove(cookie.key())) {
            cookie.data().ifPresent(data -> cookies.put(cookie.key(), data));
        } else {
            LOGGER.warn("Received unsolicited cookie with ID {} from user {}!", cookie.key(), getUsername());
        }

        if(requiredCookies.isEmpty()) {

            conn = conn.withCookies(cookies);

            cookies.clear();
            send(new ClientboundLoginFinishedPacket(profile));
        }

    }

    @Override
    public void handle(ServerboundPluginMessagePacket message) {

    }

    @Override
    public void handle(ServerboundSettingsPacket settings) {

        conn = conn.withLocale(settings.locale());

        Backend b = findBackend();

        if(b == null) {
            LOGGER.warn("Unable to find backend server for {} after login!", getUsername());
            disconnect(Component.text("Unable to find backend server!"));
            return;
        }

        reconnect(b);
    }


    private void connectToBackend(Backend b) {

        prepareForwarding();
        BackendConnection bconn = new BackendConnection(version, b.hostname(), b.port(), server.getBackendTimeout(), false);

        bconn.connect().thenAccept(ch -> {

            if(ch == null) {
                disconnect(Component.text("Unable to connect to backend!"));
                return;
            }

            bconn.changePhase(ProtocolPhase.HANDSHAKE);
            ch.write(conn.handshakePacket(ServerboundHandshakePacket.Intent.LOGIN));

            bconn.changePhase(ProtocolPhase.LOGIN);
            ch.write(conn.loginPacket());

            setupForwarding(ch);
            bconn.setupForwarding(channel);

            ch.flush();

        });
    }

    private void disconnect(Component component) {

        LOGGER.info("Disconnecting player {}: {}", getUsername(), component.allText());
        Packet<ClientboundPacketHandler> p = phase == ProtocolPhase.CONFIG ? new ClientboundConfigKickPacket(component) : new ClientboundKickPacket(component);

        channel.writeAndFlush(p).addListener(ChannelFutureListener.CLOSE);
    }

    private void startLogin() {

        if(tryConnectBackend()) {
            return;
        }

        challenge = Ints.toByteArray(rand.nextInt());
        send(new ClientboundEncryptionPacket("", server.getKeyPair().getPublic().getEncoded(), challenge, server.requiresAuth()));
    }

    private boolean tryConnectBackend() {

        Backend b = findBackend();
        if(b == null) {
            return false;
        }

        connectToBackend(b);
        return true;
    }

    private Backend findBackend() {

        backendQueue.removeIf(b -> b.canCheck(conn) && !b.canUse(conn));
        if(backendQueue.isEmpty()) return null;

        for(Backend b : backendQueue) {
            if(!b.canCheck(conn)) return null;
            if(b.canUse(conn)) {
                return b;
            }
        }

        return null;
    }

    private void setupEncryption(byte[] key) throws GeneralSecurityException {

        if(!channel.isActive()) {
            throw new IllegalStateException("Channel is not active!");
        }
        if(!channel.eventLoop().inEventLoop()) {
            throw new IllegalStateException("Attempt to enable encryption from outside the event loop!");
        }

        SecretKey secret = new SecretKeySpec(key, "AES");

        WrappedCipher decrypt = WrappedCipher.forDecryption(secret);
        WrappedCipher encrypt = WrappedCipher.forEncryption(secret);

        channel.pipeline().addBefore("frame_dec", "decrypt", new CryptDecoder(decrypt));
        channel.pipeline().addBefore("frame_enc", "encrypt", new CryptEncoder(encrypt));

        this.encrypted = true;
    }

    private void prepareForwarding() {

        channel.pipeline().remove("frame_dec");
        channel.pipeline().remove("decoder");
        channel.pipeline().remove("handler");
        channel.pipeline().remove("frame_enc");
        channel.pipeline().remove("encoder");

        if(encrypted) {
            channel.pipeline().remove("decrypt");
            channel.pipeline().remove("encrypt");
        }

        channel.config().setAutoRead(false);
    }

    private void setupForwarding(Channel forward) {

        channel.pipeline().addLast("forward", new PacketForwarder(forward));
        channel.config().setAutoRead(true);
    }

    public String getUsername() {

        return profile == null ?
                login == null ? channel.remoteAddress().toString() :
                        login.username() :
                profile.getName();

    }

    @SuppressWarnings("unchecked")
    public void changePhase(ProtocolPhase phase) {

        this.phase = phase;

        channel.pipeline().get(PacketDecoder.class).setRegistry(PacketRegistry.getServerbound(version, phase));
        channel.pipeline().get(PacketEncoder.class).setRegistry(PacketRegistry.getClientbound(version, phase));

    }

    private void reconnect(Backend b) {

        String host = b.redirect() ? b.hostname() : handshake.address();
        int port = b.redirect() ? b.port() : handshake.port();

        String id = StringUtil.randomId(16);

        send(new ClientboundSetCookiePacket(RECONNECT_COOKIE, id.getBytes()));
        send(new ClientboundTransferPacket(host, port));

        channel.config().setAutoRead(false);

        server.getReconnectCache().set(id, conn);
    }


    public void send(Packet<ClientboundPacketHandler> packet) {

        send(packet, null);
    }

    public void send(Packet<ClientboundPacketHandler> packet, ChannelFutureListener listener) {

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
}

package org.wallentines.mdproxy;

import com.google.common.primitives.Ints;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.text.ComponentResolver;
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
    private ClientConnectionImpl conn;
    private boolean encrypted;
    private GameProfile profile;
    private ProtocolPhase phase;
    private ServerboundHandshakePacket.Intent intent;

    private final Random rand = new Random();
    private final PriorityQueue<Backend> backendQueue;
    private final HashSet<Identifier> requiredCookies = new HashSet<>();
    private final HashMap<Identifier, byte[]> cookies = new HashMap<>();


    public ClientPacketHandler(Channel channel, ProxyServer server) {

        this.server = server;
        this.channel = channel;
        this.encrypted = false;
        this.phase = ProtocolPhase.HANDSHAKE;

        this.backendQueue = new PriorityQueue<>(server.getBackends());
    }

    @Override
    public void handle(ServerboundHandshakePacket handshake) {

        LOGGER.info("Received handshake from " + getUsername() + " to " + handshake.address() + " (" + handshake.intent().name() + ")");
        this.conn = new ClientConnectionImpl(handshake.protocolVersion(), handshake.address(), handshake.port());
        this.intent = handshake.intent();

        if(handshake.intent() == ServerboundHandshakePacket.Intent.STATUS) {
            changePhase(ProtocolPhase.STATUS);
        } else {
            changePhase(ProtocolPhase.LOGIN);
        }
    }

    @Override
    public void handle(ServerboundStatusPacket ping) {

        // TODO: Status
        send(new ClientboundStatusPacket(
                new GameVersion("MidnightProxy", conn.protocolVersion()),
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


        if (backendQueue.isEmpty()) {
            disconnect(server.getLangManager().component("error.no_backends", conn));
            return;
        }

        this.conn = conn.withPlayerInfo(new PlayerInfo(login.username(), login.uuid()));

        if(intent == ServerboundHandshakePacket.Intent.TRANSFER) {

            send(new ClientboundCookieRequestPacket(RECONNECT_COOKIE));

        } else {
            this.profile = new GameProfile(login.uuid(), login.username());

            // Continue with login
            startLogin();
        }
    }

    @Override
    public void handle(ServerboundEncryptionPacket encrypt) {
        if(!conn.playerInfoAvailable() || challenge == null) {
            throw new IllegalStateException("Received unrequested encryption packet!");
        }

        PrivateKey privateKey = server.getKeyPair().getPrivate();
        if(!MessageDigest.isEqual(challenge, CryptUtil.decryptData(privateKey, encrypt.verifyToken()))) {
            throw new IllegalStateException("Encryption challenge failed!");
        }

        byte[] decryptedSecret = CryptUtil.decryptData(privateKey, encrypt.sharedSecret());

        try {
            setupEncryption(decryptedSecret);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Encryption unsuccessful!");
        }

        if (server.requiresAuth()) {

            LOGGER.info("Starting authentication for {}", conn.username());

            String serverId = new BigInteger(CryptUtil.hashServerId(decryptedSecret, server.getKeyPair().getPublic())).toString(16);

            SocketAddress socketAddress = channel.remoteAddress();
            InetAddress addr = ((InetSocketAddress) socketAddress).getAddress();

            server.getAuthenticator().authenticate(this, conn.username(), serverId, addr).thenAcceptAsync(res -> finishAuthentication(res.profile()), channel.eventLoop());

        } else {
            finishAuthentication(profile);
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

            if(!newConn.username().equals(conn.username()) || !newConn.uuid().equals(conn.uuid())) {
                disconnect(server.getLangManager().component("error.invalid_reconnect", conn));
                return;
            }

            server.getReconnectCache().clear(id);
            conn = newConn;

            LOGGER.info("User {} reconnected with UUID {}", getUsername(), conn.uuid());

            if(!tryConnectBackend()) {
                disconnect(server.getLangManager().component("error.no_valid_backends", conn));
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
            LOGGER.warn("Unable to find any backend server for {} after login!", getUsername());
            disconnect(server.getLangManager().component("error.no_valid_backends", conn));
            return;
        }

        reconnect(b);
    }

    public void close() {
        channel.close();
    }

    private void connectToBackend(Backend b) {

        prepareForwarding();

        BackendConnection bconn = new BackendConnection(new GameVersion("", conn.protocolVersion()), b.hostname(), b.port(), server.getBackendTimeout(), false);
        bconn.connect(channel.eventLoop()).addListener(future -> {
            if(future.isSuccess()) {

                bconn.sendClientInformation(conn);
                bconn.setupForwarding(channel);
                setupForwarding(bconn.getChannel());

            } else {
                disconnect(server.getLangManager().component("error.backend_connection_failed", conn));
            }
        });
    }

    public void disconnect(Component component) {

        Component cmp = ComponentResolver.resolveComponent(component, conn);

        LOGGER.info("Disconnecting player {}: {}", getUsername(), cmp.allText());
        Packet<ClientboundPacketHandler> p = phase == ProtocolPhase.CONFIG ? new ClientboundConfigKickPacket(cmp) : new ClientboundKickPacket(cmp);

        channel.writeAndFlush(p).addListener(ChannelFutureListener.CLOSE);
    }

    private void startLogin() {

        if(server.getPlayerLimit() >= server.getOnlinePlayers()) {
            disconnect(server.getLangManager().component("error.server_full"));
            return;
        }

        if(tryConnectBackend()) {
            return;
        }

        if(!new GameVersion("", conn.protocolVersion()).hasFeature(GameVersion.Feature.TRANSFER_PACKETS)) {
            disconnect(server.getLangManager().component("error.cannot_transfer", conn));
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

    private void finishAuthentication(GameProfile profile) {

        this.profile = profile;
        this.conn = conn.withAuth();

        LOGGER.info("User {} signed in with UUID {}", getUsername(), profile.getId());

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

    private Backend findBackend() {

        backendQueue.removeIf(b -> b.canUse(conn) == TestResult.FAIL);
        if(backendQueue.isEmpty()) return null;

        for(Backend b : backendQueue) {
            TestResult res = b.canUse(conn);
            if(res == TestResult.NOT_ENOUGH_INFO) {
                return null;
            } else if (res == TestResult.PASS) {
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
        channel.config().setAutoRead(false);

        SecretKey secret = new SecretKeySpec(key, "AES");

        WrappedCipher decrypt = WrappedCipher.forDecryption(secret);
        WrappedCipher encrypt = WrappedCipher.forEncryption(secret);

        channel.pipeline().addBefore("frame_dec", "decrypt", new CryptDecoder(decrypt));
        channel.pipeline().addBefore("frame_enc", "encrypt", new CryptEncoder(encrypt));
        this.encrypted = true;

        channel.config().setAutoRead(true);

    }

    private void prepareForwarding() {

        channel.config().setAutoRead(false);

        channel.pipeline().remove("frame_dec");
        channel.pipeline().remove("decoder");
        channel.pipeline().remove("handler");
        channel.pipeline().remove("frame_enc");
        channel.pipeline().remove("encoder");

        if(encrypted) {
            channel.pipeline().remove("decrypt");
            channel.pipeline().remove("encrypt");
        }
    }

    private void setupForwarding(Channel forward) {

        channel.pipeline().addLast("forward", new PacketForwarder(forward));
        channel.config().setAutoRead(true);
    }

    public String getUsername() {

        return profile == null
                ? conn.playerInfoAvailable()
                        ? conn.username()
                        : channel.remoteAddress().toString()
                : profile.getName();

    }

    @SuppressWarnings("unchecked")
    public void changePhase(ProtocolPhase phase) {

        this.phase = phase;

        GameVersion version = new GameVersion("", conn.protocolVersion());

        channel.pipeline().get(PacketDecoder.class).setRegistry(PacketRegistry.getServerbound(version, phase));
        channel.pipeline().get(PacketEncoder.class).setRegistry(PacketRegistry.getClientbound(version, phase));

    }

    private void reconnect(Backend b) {

        String host = b.redirect() ? b.hostname() : conn.hostname();
        int port = b.redirect() ? b.port() : conn.port();

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

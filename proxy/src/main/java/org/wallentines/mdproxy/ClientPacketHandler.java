package org.wallentines.mdproxy;

import com.google.common.primitives.Ints;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdproxy.jwt.*;
import org.wallentines.mdproxy.netty.*;
import org.wallentines.mdproxy.packet.PacketRegistry;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;
import org.wallentines.mdproxy.packet.config.ClientboundSetCookiePacket;
import org.wallentines.mdproxy.packet.config.ClientboundTransferPacket;
import org.wallentines.mdproxy.packet.config.ServerboundPluginMessagePacket;
import org.wallentines.mdproxy.packet.config.ServerboundSettingsPacket;
import org.wallentines.mdproxy.packet.login.*;
import org.wallentines.mdproxy.packet.status.ServerboundPingPacket;
import org.wallentines.mdproxy.packet.status.ServerboundStatusPacket;
import org.wallentines.mdproxy.util.CryptUtil;
import org.wallentines.midnightlib.registry.Identifier;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.UUID;

public class ClientPacketHandler implements ServerboundPacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("ClientPacketHandler");
    private static final Identifier RECONNECT_COOKIE = new Identifier("mdp", "rc");

    private final ProxyServer server;
    private final Channel channel;
    private byte[] challenge;
    private ClientConnectionImpl conn;
    private boolean encrypted;
    private GameProfile profile;
    private ProtocolPhase phase;
    private ServerboundHandshakePacket.Intent intent;
    private StatusResponder statusResponder;

    private final Random random = new SecureRandom();
    private final HashSet<Identifier> requiredCookies = new HashSet<>();


    public ClientPacketHandler(Channel channel, ProxyServer server) {

        this.server = server;
        this.channel = channel;
        this.encrypted = false;
        this.phase = ProtocolPhase.HANDSHAKE;
    }

    @Override
    public void handle(ServerboundHandshakePacket handshake) {

        LOGGER.info("Received handshake from " + getUsername() + " to " + handshake.address() + " (" + handshake.intent().name() + ")");
        this.conn = new ClientConnectionImpl(channel, handshake.protocolVersion(), handshake.address(), handshake.port());
        this.intent = handshake.intent();

        if(handshake.intent() == ServerboundHandshakePacket.Intent.STATUS) {
            changePhase(ProtocolPhase.STATUS);
        } else {
            changePhase(ProtocolPhase.LOGIN);
        }
    }

    @Override
    public void handle(ServerboundStatusPacket ping) {

        PriorityQueue<StatusEntry> ent = new PriorityQueue<>(server.getStatusEntries());
        for(StatusEntry e : ent) {
            if(e.canUse(new ConnectionContext(conn, server))) {

                statusResponder = new StatusResponder(conn, server, e);
                statusResponder.status(new GameVersion("MidnightProxy", conn.protocolVersion()));

                break;
            }
        }
    }

    @Override
    public void handle(ServerboundPingPacket ping) {

        if(statusResponder == null) {
            LOGGER.warn("Received ping packet before status packet!");
            return;
        }
        statusResponder.ping(ping);
    }

    @Override
    public void handle(ServerboundLoginPacket login) {

        if (server.getRoutes().isEmpty()) {
            disconnect(server.getLangManager().component("error.no_backends", conn));
            return;
        }

        conn.setPlayerInfo(new PlayerInfo(login.username(), login.uuid()));

        if(intent == ServerboundHandshakePacket.Intent.TRANSFER) {

            conn.send(new ClientboundCookieRequestPacket(RECONNECT_COOKIE));

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

        if (server.isOnlineMode()) {

            LOGGER.info("Starting authentication for {}", conn.username());

            String serverId = new BigInteger(CryptUtil.hashServerId(decryptedSecret, server.getKeyPair().getPublic())).toString(16);
            server.getAuthenticator().authenticate(conn, serverId).thenAcceptAsync(res -> finishAuthentication(res.profile()), channel.eventLoop());

        } else {
            finishAuthentication(profile);
        }
    }

    @Override
    public void handle(ServerboundLoginFinishedPacket finished) {

        changePhase(ProtocolPhase.CONFIG);
    }

    @Override
    public void handle(ServerboundCookiePacket cookie) {

        if(cookie.key().equals(RECONNECT_COOKIE)) {

            String jwt = cookie.data().map(bytes -> new String(cookie.data().orElseThrow(), StandardCharsets.US_ASCII)).orElse(null);
            if(jwt == null || jwt.isEmpty()) {
                startLogin();
                return;
            }

            SerializeResult<JWT> jwtRes = JWTReader.readAny(jwt, KeySupplier.of(server.getReconnectKeyPair().getPrivate(), KeyType.RSA_PRIVATE));
            if(!jwtRes.isComplete()) {

                disconnect(server.getLangManager().component("error.invalid_reconnect", conn));
                return;
            }

            JWT decoded = jwtRes.getOrThrow();
            if(decoded.isExpired()) {
                startLogin();
                return;
            }

            // Verify claims
            JWTVerifier verifier = new JWTVerifier()
                    .requireEncrypted()
                    .withClaim("hostname", conn.hostname())
                    .withClaim("port", conn.port())
                    .withClaim("username", conn.username())
                    .withClaim("uuid", conn.uuid().toString())
                    .withClaim("protocol", conn.protocolVersion());

            if(!verifier.verify(decoded)) {
                disconnect(server.getLangManager().component("error.invalid_reconnect", conn));
                return;
            }

            Backend b = server.getBackends().get(decoded.getClaim("backend").asString());
            if(b == null) {
                disconnect(server.getLangManager().component("error.invalid_reconnect", conn));
                return;
            }

            connectToBackend(b);
            return;
        }


        if(requiredCookies.remove(cookie.key())) {
            cookie.data().ifPresent(data -> conn.setCookie(cookie.key(), data));
        } else {
            LOGGER.warn("Received unsolicited cookie with ID {} from user {}!", cookie.key(), getUsername());
        }

        if(requiredCookies.isEmpty()) {
            conn.send(new ClientboundLoginFinishedPacket(profile));
        }

    }

    @Override
    public void handle(ServerboundPluginMessagePacket message) {

    }

    @Override
    public void handle(ServerboundSettingsPacket settings) {

        conn.setLocale(settings.locale());

        Backend b = findBackend();
        if(conn.hasDisconnected()) {
            return;
        }

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

        if(conn.hasDisconnected()) return;

        prepareForwarding();

        UUID uuid = conn.uuid();
        channel.closeFuture().addListener(future -> {
            server.removePlayer(uuid);
        });
        server.addPlayer(conn);


        BackendConnectionImpl bconn = new BackendConnectionImpl(conn, b, new GameVersion("", conn.protocolVersion()), server.getBackendTimeout());
        bconn.connect(channel.eventLoop()).addListener(future -> {
            if(future.isSuccess()) {

                conn.setBackend(bconn);

                bconn.send(conn.handshakePacket(ServerboundHandshakePacket.Intent.LOGIN));

                bconn.changePhase(ProtocolPhase.LOGIN);
                bconn.send(conn.loginPacket());

                bconn.setupForwarding(channel);
                setupForwarding(bconn.getChannel());

            } else {
                disconnect(server.getLangManager().component("error.backend_connection_failed", conn));
            }
        });
    }

    private void startLogin() {

        boolean canConnectImmediately = true;

        if(server.requiresAuth()) {
            canConnectImmediately = false;
        } else if(server.getOnlinePlayers() >= server.getPlayerLimit()) {
            switch (conn.bypassesPlayerLimit(server)) {
                case FAIL -> {
                    disconnect(server.getLangManager().component("error.server_full"));
                    return;
                }
                case NOT_ENOUGH_INFO -> canConnectImmediately = false;
            }
        }

        if(canConnectImmediately && tryConnectBackend()) {
            return;
        }

        if(conn.hasDisconnected()) {
            return;
        }

        if(!new GameVersion("", conn.protocolVersion()).hasFeature(GameVersion.Feature.TRANSFER_PACKETS)) {
            disconnect(server.getLangManager().component("error.cannot_transfer", conn));
            return;
        }

        challenge = Ints.toByteArray(random.nextInt());
        conn.send(new ClientboundEncryptionPacket("", server.getKeyPair().getPublic().getEncoded(), challenge, server.isOnlineMode()));
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
        conn.setAuthenticated(true);

        LOGGER.info("User {} signed in with UUID {}", getUsername(), profile.getId());

        if(server.getOnlinePlayers() >= server.getPlayerLimit()) {
            if(conn.bypassesPlayerLimit(server) != TestResult.PASS) {
                disconnect(server.getLangManager().component("error.server_full"));
                return;
            }
        }

        for (Route b : server.getRoutes()) {
            requiredCookies.addAll(b.getRequiredCookies());
        }

        if (requiredCookies.isEmpty()) {
            conn.send(new ClientboundLoginFinishedPacket(profile));

        } else {
            for(Identifier i : requiredCookies) {
                conn.send(new ClientboundCookieRequestPacket(i));
            }
        }

    }

    private void disconnect(Component component) {
        conn.disconnect(phase, component);
    }

    private Backend findBackend() {

        if(server.getRoutes().isEmpty()) {
            return null;
        }

        for(Route r : server.getRoutes()) {

            if(!channel.isActive()) {
                return null;
            }

            ConnectionContext ctx = new ConnectionContext(conn, server);
            TestResult res = r.canUse(ctx);
            if(res == TestResult.NOT_ENOUGH_INFO) {
                return null;
            } else if (res == TestResult.PASS) {

                return r.resolveBackend(ctx, server.getBackends());
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

        LOGGER.info("User {} connected to backend {}", conn.username(), server.getBackends().getId(conn.getBackendConnection().getBackend()));
        channel.pipeline().addLast("forward", new PacketForwarder(forward));
        channel.config().setAutoRead(true);
    }

    public String getUsername() {

        return profile == null
                ? conn != null && conn.playerInfoAvailable()
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

        KeyCodec<PublicKey, PrivateKey> rsa = KeyCodec.RSA_OAEP(server.getReconnectKeyPair());
        String str = new JWTBuilder()
                .withClaim("hostname", host)
                .withClaim("port", port)
                .withClaim("protocol", conn.protocolVersion())
                .withClaim("username", conn.username())
                .withClaim("uuid", conn.uuid().toString())
                .withClaim("backend", server.getBackends().getId(b))
                .expiresIn(server.getReconnectTimeout() / 1000)
                .issuedBy("midnightproxy")
                .encrypted(rsa, CryptCodec.A128CBC_HS256())
                .asString().getOrThrow();

        conn.send(new ClientboundSetCookiePacket(RECONNECT_COOKIE, str.getBytes()));
        conn.send(new ClientboundTransferPacket(host, port));
    }



}

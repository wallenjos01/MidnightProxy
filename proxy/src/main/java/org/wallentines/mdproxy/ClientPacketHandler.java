package org.wallentines.mdproxy;

import com.google.common.primitives.Ints;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.lang.UnresolvedComponent;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.jwt.*;
import org.wallentines.mdproxy.netty.*;
import org.wallentines.mdproxy.packet.PacketRegistry;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;
import org.wallentines.mdproxy.packet.common.ClientboundCookieRequestPacket;
import org.wallentines.mdproxy.packet.common.ServerboundCookiePacket;
import org.wallentines.mdproxy.packet.config.ClientboundSetCookiePacket;
import org.wallentines.mdproxy.packet.config.ClientboundTransferPacket;
import org.wallentines.mdproxy.packet.config.ServerboundPluginMessagePacket;
import org.wallentines.mdproxy.packet.config.ServerboundSettingsPacket;
import org.wallentines.mdproxy.packet.login.*;
import org.wallentines.mdproxy.packet.status.ServerboundPingPacket;
import org.wallentines.mdproxy.packet.status.ServerboundStatusPacket;
import org.wallentines.mdproxy.util.CryptUtil;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.types.DefaultedSingleton;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public class ClientPacketHandler implements ServerboundPacketHandler {


    private static final Component IGNORE_STATUS_REASON = Component.translate("disconnect.ignoring_status_request");

    private static final Logger LOGGER = LoggerFactory.getLogger("ClientPacketHandler");
    private static final Identifier RECONNECT_COOKIE = new Identifier("mdp", "rc");

    private final ProxyServer server;
    private final Channel channel;
    private byte[] challenge;
    private ClientConnectionImpl conn;
    private boolean encrypted;
    private PlayerProfile profile;
    private ServerboundHandshakePacket.Intent intent;
    private StatusResponder statusResponder;

    private final Random random = new SecureRandom();
    private final Queue<Route> routes;
    private final HashSet<Identifier> requestedCookies = new HashSet<>();
    private final DefaultedSingleton<InetSocketAddress> address;

    private boolean wasReconnected = false;


    public ClientPacketHandler(Channel channel, DefaultedSingleton<InetSocketAddress> address, ProxyServer server) {

        this.server = server;
        this.channel = channel;
        this.encrypted = false;
        this.address = address;

        this.routes = new ArrayDeque<>(server.getRoutes());
    }

    @Nullable
    public ClientConnectionImpl getConnection() {
        return conn;
    }

    @Override
    public void handle(ServerboundHandshakePacket handshake) {

        if(handshake.intent() != ServerboundHandshakePacket.Intent.STATUS || server.logStatusMessages()) {
            LOGGER.info("Received handshake from {} to {} ({})", getUsername(), handshake.address(), handshake.intent().name());
        }

        this.conn = new ClientConnectionImpl(channel, address.get(), handshake.protocolVersion(), handshake.address(), handshake.port());
        this.intent = handshake.intent();

        this.server.clientConnectEvent().invoke(conn);

        if(handshake.intent() == ServerboundHandshakePacket.Intent.STATUS) {
            changePhase(ProtocolPhase.STATUS);
        } else {
            changePhase(ProtocolPhase.LOGIN);
        }
    }

    @Override
    public void handle(ServerboundStatusPacket ping) {

        StatusEntry e = conn.getStatusEntry(server);
        if(e == null) {
            conn.disconnect(IGNORE_STATUS_REASON, false);
            return;
        }

        statusResponder = new StatusResponder(conn, server, e);
        statusResponder.status(new GameVersion("MidnightProxy", conn.protocolVersion()));
    }

    @Override
    public void handle(ServerboundPingPacket ping) {

        if(statusResponder == null) {
            LOGGER.warn("Received ping packet before status packet!");
            conn.disconnect(IGNORE_STATUS_REASON, false);
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
            this.profile = new PlayerProfile(login.uuid(), login.username());

            // Continue with login
            preLogin();
        }
    }

    @Override
    public void handle(ServerboundLoginQueryPacket message) {
        conn.loginQueryEvent().invoke(message);
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
            server.getAuthenticator().authenticate(conn, serverId).thenAcceptAsync(this::finishAuthentication, channel.eventLoop());

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

            if(cookie.data().length == 0) {
                preLogin();
                return;
            }

            PlayerInfo pi = conn.playerInfo();
            if(pi == null) {
                LOGGER.error("Received reconnect cookie before player info!");
                disconnect(server.getLangManager().component("error.invalid_reconnect", conn));
                return;
            }

            String jwt = new String(cookie.data(), StandardCharsets.US_ASCII);
            SerializeResult<JWT> jwtRes = JWTReader.readAny(jwt, KeySupplier.of(server.getReconnectKeyPair().getPrivate(), KeyType.RSA_PRIVATE));
            if(!jwtRes.isComplete()) {

                LOGGER.warn("Unable to parse reconnect cookie! {}", jwtRes.getError());
                disconnect(server.getLangManager().component("error.invalid_reconnect", conn));
                return;
            }

            JWT decoded = jwtRes.getOrThrow();
            if(decoded.isExpired()) {
                preLogin();
                return;
            }

            // Verify claims
            JWTVerifier verifier = new JWTVerifier()
                    .requireEncrypted()
                    .enforceSingleUse(server.getTokenCache())
                    .withClaim("hostname", conn.hostname())
                    .withClaim("port", conn.port())
                    .withClaim("username", pi.username())
                    .withClaim("uuid", pi.uuid().toString())
                    .withClaim("protocol", conn.protocolVersion());

            if(!verifier.verify(decoded)) {
                LOGGER.warn("Unable to verify reconnect cookie!");
                disconnect(server.getLangManager().component("error.invalid_reconnect", conn));
                return;
            }

            Backend b;
            String backendId = decoded.getClaimAsString("backend");
            if(backendId == null) {
                // Check for ephemeral backend
                String ephHost = decoded.getClaimAsString("backend_host");
                ConfigObject ephPort = decoded.getClaim("backend_port");
                ConfigObject ephRedirect = decoded.getClaim("backend_redirect");
                ConfigObject ephHaproxy = decoded.getClaim("backend_haproxy");
                if(ephHost == null
                        || ephPort == null || !ephPort.isNumber()
                        || ephRedirect == null || !ephRedirect.isBoolean()
                        || ephHaproxy == null || !ephHaproxy.isBoolean()) {
                    LOGGER.warn("Unable to find any reconnect backend!");
                    disconnect(server.getLangManager().component("error.invalid_reconnect", conn));
                    return;
                }
                b = new Backend(ephHost, ephPort.asNumber().intValue(), ephRedirect.asBoolean(), ephHaproxy.asBoolean());

            } else {
                b = server.getBackends().get(backendId);
                if(b == null) {
                    LOGGER.warn("Unable to find requested reconnect backend {}!", backendId);
                    disconnect(server.getLangManager().component("error.invalid_reconnect", conn));
                    return;
                }
            }

            connectToBackend(b);
            return;
        }

        if(requestedCookies.remove(cookie.key())) {
            conn.setCookie(cookie.key(), cookie.data());
        } else {
            LOGGER.warn("Received unsolicited cookie with ID {} from user {}!", cookie.key(), getUsername());
        }

        if(requestedCookies.isEmpty()) {
            tryNextServer();
        }

    }

    @Override
    public void handle(ServerboundPluginMessagePacket message) {
        conn.pluginMessageEvent().invoke(message);
    }

    @Override
    public void handle(ServerboundSettingsPacket settings) {

        conn.setLocale(settings.locale());
        conn.executeTasksAsync(Task.CONFIGURE_QUEUE).thenRun(this::tryNextServer);

    }

    public void close() {
        channel.close();
    }

    private void connectToBackend(Backend b) {

        if(conn.hasDisconnected()) return;
        if(conn.authenticated()) {
            reconnect(b);
            return;
        }

        conn.executeTasksAsync(Task.PRE_BACKEND_CONNECT).thenRun(() -> {

            channel.config().setAutoRead(false);

            server.getConnectionManager()
                    .connectToBackend(conn, b, new GameVersion("", conn.protocolVersion()), server.getBackendTimeout())
                    .thenAccept(bconn -> {
                        ((PlayerListImpl) server.getPlayerList()).addPlayer(conn);

                        conn.setBackend(bconn);

                        bconn.send(conn.handshakePacket(ServerboundHandshakePacket.Intent.LOGIN));

                        bconn.changePhase(ProtocolPhase.LOGIN);
                        bconn.send(conn.loginPacket());

                        bconn.setupForwarding(channel);
                        setupForwarding(bconn.getChannel());

                        server.clientJoinBackendEvent().invoke(conn);
                        conn.executeTasksAsync(Task.POST_BACKEND_CONNECT);
                    })
                    .exceptionally(ex -> {
                        LOGGER.error("An error occurred while connecting to a backend server!", ex);
                        disconnect(server.getLangManager().component("error.backend_connection_failed", conn));
                        return null;
                    });
        });
    }

    private void preLogin() {

        conn.executeTasksAsync(Task.PRE_LOGIN_QUEUE).thenRun(this::startLogin);
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

        if(canConnectImmediately) {
            tryNextServer();
            return;
        }

        if(conn.hasDisconnected()) {
            return;
        }

        startAuthentication();
    }


    private void startAuthentication() {

        if(conn.authenticated()) {
            throw new IllegalStateException("Attempt to re-authenticate player");
        }

        if(!new GameVersion("", conn.protocolVersion()).hasFeature(GameVersion.Feature.TRANSFER_PACKETS)) {
            disconnect(server.getLangManager().component("error.cannot_transfer", conn));
            return;
        }

        challenge = Ints.toByteArray(random.nextInt());
        conn.send(new ClientboundEncryptionPacket("", server.getKeyPair().getPublic().getEncoded(), challenge, server.isOnlineMode()));
    }

    private void finishAuthentication(PlayerProfile profile) {

        this.profile = profile;
        conn.setAuthenticated(true);

        LOGGER.info("User {} signed in with UUID {}", getUsername(), profile.uuid());

        if(server.getOnlinePlayers() >= server.getPlayerLimit()) {
            if(conn.bypassesPlayerLimit(server) != TestResult.PASS) {
                disconnect(server.getLangManager().component("error.server_full"));
                return;
            }
        }

        conn.executeTasksAsync(Task.POST_LOGIN_QUEUE).thenRun(() -> {
            conn.send(new ClientboundLoginFinishedPacket(profile));
        });

    }

    private void disconnect(UnresolvedComponent component) {
        conn.disconnect(component);
    }

    private void tryNextServer() {

        Backend toUse = null;
        while(!routes.isEmpty()) {

            Route current = routes.peek();
            if (conn.authenticated() || current.requirement() != null && !current.requirement().requiresAuth()) {
                for (Identifier id : current.getRequiredCookies()) {
                    if (conn.getCookie(id) == null) {
                        requestedCookies.add(id);
                        conn.send(new ClientboundCookieRequestPacket(id));
                    }
                }
                if(!requestedCookies.isEmpty()) {
                    return;
                }
            }

            ConnectionContext ctx = new ConnectionContext(conn, server);
            TestResult res = current.canUse(ctx);

            if (conn.hasDisconnected()) {
                return;
            }

            if(res == TestResult.NOT_ENOUGH_INFO) {
                startAuthentication();
                return;
            }

            if(res == TestResult.PASS) {
                toUse = current.resolveBackend(ctx, server.getBackends());
                if(toUse == null) {
                    LOGGER.warn("Unable to resolve backend for successful route! ({})", current.backend());
                } else {
                    break;
                }
            }

            if(res == TestResult.FAIL && current.kickOnFail()) {
                disconnect(server.getLangManager().component(current.kickMessage(), conn));
                return;
            }

            routes.remove();
        }

        if(toUse == null) {
            LOGGER.warn("Unable to find any backend server for {}!", getUsername());
            disconnect(server.getLangManager().component("error.no_valid_backends", conn));
            return;
        }

        connectToBackend(toUse);
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

    private void setupForwarding(Channel forward) {

        channel.pipeline().remove("frame_dec");
        channel.pipeline().remove("decoder");
        channel.pipeline().remove("handler");
        channel.pipeline().remove("frame_enc");
        channel.pipeline().remove("encoder");

        if(encrypted) {
            channel.pipeline().remove("decrypt");
            channel.pipeline().remove("encrypt");
        }

        channel.pipeline().addLast("forward", new PacketForwarder(forward));
        channel.config().setAutoRead(true);

        LOGGER.info("User {} connected to backend {}", conn.username(), server.getBackends().getId(conn.getBackendConnection().getBackend()));
    }

    public String getUsername() {

        return profile == null
                ? conn != null && conn.playerInfoAvailable()
                        ? conn.username()
                        : channel.remoteAddress().toString()
                : profile.username();

    }

    public boolean wasReconnected() {
        return wasReconnected;
    }

    @SuppressWarnings("unchecked")
    public void changePhase(ProtocolPhase phase) {

        GameVersion version = new GameVersion("", conn.protocolVersion());

        channel.pipeline().get(PacketDecoder.class).setRegistry(PacketRegistry.getServerbound(version, phase));
        channel.pipeline().get(PacketEncoder.class).setRegistry(PacketRegistry.getClientbound(version, phase));

    }

    private void reconnect(Backend b) {

        String host = b.redirect() ? b.hostname() : conn.hostname();
        int port = b.redirect() ? b.port() : conn.port();

        KeyCodec<PublicKey, PrivateKey> rsa = KeyCodec.RSA_OAEP(server.getReconnectKeyPair());
        String backendId = server.getBackends().getId(b);
        JWTBuilder builder = new JWTBuilder()
                .withClaim("hostname", host)
                .withClaim("port", port)
                .withClaim("protocol", conn.protocolVersion())
                .withClaim("username", conn.username())
                .withClaim("uuid", Objects.toString(conn.uuid()))
                .withClaim(server.getTokenCache().getIdClaim(), UUID.randomUUID(), Serializer.UUID)
                .expiresIn(server.getReconnectTimeout())
                .issuedNow()
                .issuedBy("midnightproxy");

        // Ephemeral Backend
        if(backendId == null) {
            backendId = b.toString();
            builder.withClaim("backend_host", b.hostname())
                    .withClaim("backend_port", b.port())
                    .withClaim("backend_redirect", b.redirect())
                    .withClaim("backend_haproxy", b.haproxy());
        } else {
            builder.withClaim("backend", backendId);
        }

        String str = builder
                .encrypted(rsa, CryptCodec.A128CBC_HS256())
                .asString().getOrThrow();

        wasReconnected = true;
        LOGGER.info("Reconnecting {} to {}", getUsername(), backendId);

        conn.send(new ClientboundSetCookiePacket(RECONNECT_COOKIE, str.getBytes()));
        conn.send(new ClientboundTransferPacket(host, port), ChannelFutureListener.CLOSE);

    }



}

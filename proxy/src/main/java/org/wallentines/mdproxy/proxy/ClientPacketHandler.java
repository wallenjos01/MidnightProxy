package org.wallentines.mdproxy.proxy;

import com.google.common.primitives.Ints;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdproxy.Backend;
import org.wallentines.mdproxy.ClientConnectionImpl;
import org.wallentines.mdproxy.packet.*;
import org.wallentines.mdproxy.packet.config.ClientboundConfigKickPacket;
import org.wallentines.mdproxy.packet.config.ClientboundSetCookiePacket;
import org.wallentines.mdproxy.packet.config.ClientboundTransferPacket;
import org.wallentines.mdproxy.packet.login.*;
import org.wallentines.mdproxy.packet.status.ClientboundStatusPacket;
import org.wallentines.mdproxy.packet.status.ServerboundStatusPacket;
import org.wallentines.mdproxy.packet.status.StatusPingPacket;
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

public class ClientPacketHandler extends SimpleChannelInboundHandler<Packet> {

    private static final Logger LOGGER = LoggerFactory.getLogger("ClientPacketHandler");
    private static final Identifier RECONNECT_COOKIE = new Identifier("mdp", "rid");

    private final ProxyServer server;
    private final Channel channel;
    private byte[] challenge;

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

        this.server = server;
        this.channel = channel;
        this.encrypted = false;
        this.phase = ProtocolPhase.HANDSHAKE;

        this.backendQueue = new PriorityQueue<>(server.getBackends());
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        try {
            handlePacket(packet);
        } finally {
            ReferenceCountUtil.release(packet);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("An exception occurred while handling a packet!", cause);
        channel.close();
    }

    private void handlePacket(Packet packet) throws Exception {
        if(packet instanceof ServerboundHandshakePacket h) {

            LOGGER.info("Received handshake from " + getUsername() + " to " + h.address() + " (" + h.intent().name() + ")");
            this.handshake = h;
            if(h.intent() == ServerboundHandshakePacket.Intent.STATUS) {

                changePhase(ProtocolPhase.STATUS);

            } else {

                changePhase(ProtocolPhase.LOGIN);
            }
        }
        else if(packet instanceof ServerboundStatusPacket) {

            send(new ClientboundStatusPacket(
                    new GameVersion("MidnightProxy", handshake.protocolVersion()),
                    Component.text("A MidnightProxy Server"),
                    100, 0,
                    false, false
            ));

        }
        else if(packet instanceof StatusPingPacket sp) {
            send(sp);
        }
        else if(packet instanceof ServerboundLoginPacket l) {

            GameVersion ver = new GameVersion("", handshake.protocolVersion());
            if (!ver.hasFeature(GameVersion.Feature.TRANSFER_PACKETS)) {
                disconnect(Component.text("This server requires at least version 1.20.5! (24w03a)"));
                return;
            }

            if (backendQueue.isEmpty()) {
                disconnect(Component.text("There are no backend servers available!"));
                return;
            }

            this.login = l;

            if(handshake.intent() == ServerboundHandshakePacket.Intent.TRANSFER) {

                send(new ClientboundCookieRequestPacket(RECONNECT_COOKIE));

            } else {
                this.conn = new ClientConnectionImpl(handshake.protocolVersion(), handshake.address(), handshake.port(), login.username(), login.uuid());
                this.profile = new GameProfile(login.uuid(), login.username());

                // Continue with login
                startLogin();
            }
        }
        else if(packet instanceof ServerboundEncryptionPacket e) {

            if(login == null || challenge == null) {
                throw new IllegalStateException("Received unrequested encryption packet!");
            }

            PrivateKey privateKey = server.getKeyPair().getPrivate();
            if(!MessageDigest.isEqual(challenge, CryptUtil.decryptData(privateKey, e.verifyToken()))) {
                throw new IllegalStateException("Encryption unsuccessful!");
            }

            byte[] decryptedSecret = CryptUtil.decryptData(privateKey, e.sharedSecret());
            if (server.requiresAuth()) {

                LOGGER.info("Starting authentication for " + login.username());

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

            setupEncryption(decryptedSecret);

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
        else if(packet instanceof ServerboundCookiePacket c) {

            if(c.key().equals(RECONNECT_COOKIE)) {

                String id = c.data().map(bytes -> new String(c.data().orElseThrow(), StandardCharsets.US_ASCII)).orElse(null);
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


            if(requiredCookies.remove(c.key())) {
                c.data().ifPresent(data -> cookies.put(c.key(), data));
            } else {
                LOGGER.warn("Received unsolicited cookie with ID " + c.key() + " from user " + getUsername());
            }

            if(requiredCookies.isEmpty()) {

                conn = conn.withCookies(cookies);

                cookies.clear();
                send(new ClientboundLoginFinishedPacket(profile));
            }
        }
        else if(packet instanceof ServerboundLoginFinishedPacket) {

            changePhase(ProtocolPhase.CONFIG);

            // Pick Server, Transfer
            conn = conn.withTransferable();
            Backend b = findBackend();

            if(b == null) {
                LOGGER.warn("Unable to find backend after login!");
                disconnect(Component.text("Unable to find backend server!"));
                return;
            }

            String host = b.redirect() ? b.hostname() : handshake.address();
            int port = b.redirect() ? b.port() : handshake.port();

            String id = StringUtil.randomId(16);

            send(new ClientboundSetCookiePacket(RECONNECT_COOKIE, id.getBytes()));
            send(new ClientboundTransferPacket(host, port));

            channel.config().setAutoRead(false);

            server.getReconnectCache().set(id, conn);
        }
    }


    private void connectToBackend(Backend b) {

        prepareForwarding();
        BackendConnection bconn = new BackendConnection(b.hostname(), b.port(), false);

        bconn.connect().thenAccept(ch -> {

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
        Packet p = phase == ProtocolPhase.CONFIG ? new ClientboundConfigKickPacket(component) : new ClientboundKickPacket(component);

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

    protected void setupEncryption(byte[] key) throws GeneralSecurityException {

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

    protected void prepareForwarding() {

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
    }

    public String getUsername() {

        return profile == null ?
                login == null ? channel.remoteAddress().toString() :
                        login.username() :
                profile.getName();

    }

    public void changePhase(ProtocolPhase phase) {

        this.phase = phase;

        channel.pipeline().get(PacketDecoder.class).setRegistry(PacketRegistry.getRegistry(PacketFlow.SERVERBOUND, phase));
        channel.pipeline().get(PacketEncoder.class).setRegistry(PacketRegistry.getRegistry(PacketFlow.CLIENTBOUND, phase));

    }


    public void send(Packet packet) {

        send(packet, null);
    }

    public void send(Packet packet, ChannelFutureListener listener) {

        if(channel.eventLoop().inEventLoop()) {
            doSend(packet, listener);
        } else {
            channel.eventLoop().execute(() -> doSend(packet, listener));
        }
    }

    private void doSend(Packet packet, ChannelFutureListener listener) {

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

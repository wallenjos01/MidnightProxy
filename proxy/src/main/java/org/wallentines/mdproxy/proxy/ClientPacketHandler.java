package org.wallentines.mdproxy.proxy;

import com.google.common.primitives.Ints;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import io.netty.channel.*;
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
import org.wallentines.midnightlib.registry.Identifier;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.*;

public class ClientPacketHandler extends SimpleChannelInboundHandler<Packet> {

    private static final Logger LOGGER = LoggerFactory.getLogger("ClientPacketHandler");
    private static final Identifier RECONNECT_COOKIE = new Identifier("mdp", "rid");

    private final ProxyServer server;
    private final Channel channel;

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

        if(packet instanceof ServerboundHandshakePacket h) {

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

            this.login = l;
            this.conn = new ClientConnectionImpl(handshake.address(), handshake.port(), login.username(), login.uuid());

            this.profile = new GameProfile(login.uuid(), login.username());

            if (backendQueue.isEmpty()) {
                disconnect(Component.text("There are no backend servers available!"));
                return;
            }

            if(handshake.intent() == ServerboundHandshakePacket.Intent.TRANSFER) {
                // Check for reconnect
                send(new ClientboundCookieRequestPacket(RECONNECT_COOKIE));

            } else {

                // Continue with login
                startLogin();
            }
        }
        else if(packet instanceof ServerboundEncryptionPacket e) {

            PrivateKey privateKey = server.getKeyPair().getPrivate();
            SecretKey key = new SecretKeySpec(CryptUtil.decryptData(privateKey, e.sharedSecret()), "AES");
            String serverId = new BigInteger(CryptUtil.hashData(key.getEncoded(), server.getKeyPair().getPublic().getEncoded())).toString(16);

            SocketAddress socketAddress = ctx.channel().remoteAddress();
            InetAddress addr = ((InetSocketAddress) socketAddress).getAddress();

            setupEncryption(ctx, key);

            if (server.requiresAuth()) {

                try {

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

            for (Backend b : backendQueue) {
                requiredCookies.addAll(b.getRequiredCookies());
            }

            if (requiredCookies.isEmpty()) {
                LOGGER.warn("No cookies required - sending login finished packet");
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
                ClientConnectionImpl newConn = id == null ? null : server.getReconnectData(id);

                if(newConn == null) {
                    startLogin();
                    return;
                }

                if(!newConn.username().equals(login.username()) || !newConn.uuid().equals(login.uuid())) {
                    disconnect(Component.text("Invalid reconnection!"));
                    return;
                }

                server.clearReconnect(id);
                conn = newConn;

                LOGGER.info("User {} reconnected with UUID {}", getUsername(), profile.getId());

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

            LOGGER.warn("Login finished, transitioning to config...");
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

            String randId = Integer.toHexString(rand.nextInt());

            LOGGER.warn("Requesting reconnect cookie");
            send(new ClientboundSetCookiePacket(RECONNECT_COOKIE, randId.getBytes(StandardCharsets.US_ASCII)));
            send(new ClientboundTransferPacket(host, port));

        }
    }

    private void connectToBackend(Backend b) {

        BackendConnection conn = new BackendConnection(b.hostname(), b.port(), false);
        conn.connect().thenAccept(ch -> {
            conn.changePhase(ProtocolPhase.HANDSHAKE);
            ch.write(handshake);
            conn.changePhase(ProtocolPhase.LOGIN);
            ch.write(login);
            setupForwarding(ch);
            conn.setupForwarding(channel);
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

        byte[] challenge = Ints.toByteArray(rand.nextInt());

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

    protected void setupEncryption(ChannelHandlerContext ctx, SecretKey key) {

        Cipher encrypt = CryptUtil.getCipher(Cipher.ENCRYPT_MODE, key);
        Cipher decrypt = CryptUtil.getCipher(Cipher.DECRYPT_MODE, key);

        ctx.pipeline().addBefore("splitter", "decrypt", new CryptDecoder(decrypt));
        ctx.pipeline().addBefore("prepender", "encrypt", new CryptEncoder(encrypt));

        this.encrypted = true;
    }

    protected void setupForwarding(Channel forward) {

        try {
            channel.pipeline().remove("splitter");
            channel.pipeline().remove("decoder");
            channel.pipeline().remove("handler");
            channel.pipeline().remove("prepender");
            channel.pipeline().remove("encoder");
            
            if(encrypted) {
                channel.pipeline().remove("decrypt");
                channel.pipeline().remove("encrypt");
            }

            channel.pipeline().addLast("forward", new PacketForwarder(forward));

        } catch (Exception ex) {

            LOGGER.error("An exception occurred while establishing a backend connection!", ex);
            channel.close();
        }

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

        ChannelFuture future = channel.writeAndFlush(packet);

        if(listener != null) {
            future.addListener(listener);
        }
    }

}

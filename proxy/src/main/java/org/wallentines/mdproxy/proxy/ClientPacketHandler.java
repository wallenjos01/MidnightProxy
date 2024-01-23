package org.wallentines.mdproxy.proxy;

import com.google.common.primitives.Ints;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import io.netty.channel.*;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdproxy.ClientConnectionImpl;
import org.wallentines.mdproxy.packet.*;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.login.ClientboundEncryptionPacket;
import org.wallentines.mdproxy.packet.login.ClientboundKickPacket;
import org.wallentines.mdproxy.packet.login.ServerboundEncryptionPacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginPacket;
import org.wallentines.mdproxy.packet.status.ClientboundStatusPacket;
import org.wallentines.mdproxy.packet.status.ServerboundStatusPacket;
import org.wallentines.mdproxy.util.CryptUtil;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Random;

public class ClientPacketHandler extends SimpleChannelInboundHandler<Packet> {

    private static final String SERVER_ID = "MDP";

    private ServerboundHandshakePacket handshake;
    private ServerboundLoginPacket login;
    private final ProxyServer server;
    private final Channel channel;
    private ClientConnectionImpl conn;

    public ClientPacketHandler(Channel channel, ProxyServer server) {
        this.server = server;
        this.channel = channel;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {

        if(packet instanceof ServerboundHandshakePacket h) {

            this.handshake = h;
            if(h.intent() == ServerboundHandshakePacket.Intent.STATUS) {

                changeState(ctx, PacketRegistry.STATUS_SERVERBOUND);

            } else {

                changeState(ctx, PacketRegistry.LOGIN_SERVERBOUND);
            }
        }
        else if(packet instanceof ServerboundStatusPacket) {

            send(new ClientboundStatusPacket(
                    new GameVersion("MidnightProxy", handshake.protocolVersion()),
                    Component.text("A MidnightProxy Server"),
                    100, 0,
                    false, false
            ), ChannelFutureListener.CLOSE);

        }
        else if(packet instanceof ServerboundLoginPacket l) {

            this.login = l;
            this.conn = new ClientConnectionImpl(handshake.address(), handshake.port(), login.username(), login.uuid());

            Random rand = new Random();
            byte[] challenge = Ints.toByteArray(rand.nextInt());

            send(new ClientboundEncryptionPacket("MDB", server.getKeyPair().getPublic().getEncoded(), challenge, server.requiresAuth()));

        }
        else if(packet instanceof ServerboundEncryptionPacket e) {

            PrivateKey privateKey = server.getKeyPair().getPrivate();
            SecretKey key = new SecretKeySpec(CryptUtil.decryptData(privateKey, e.sharedSecret()), "AES");
            String serverId = new BigInteger(CryptUtil.hashData(SERVER_ID.getBytes("ISO_8859_1"), server.getKeyPair().getPublic().getEncoded(), key.getEncoded())).toString(16);

            SocketAddress socketAddress = ctx.channel().remoteAddress();
            InetAddress addr = ((InetSocketAddress) socketAddress).getAddress();

            setupEncryption(ctx, key);

            if(server.requiresAuth()) {

                // TODO: Threading
                try {

                    ProfileResult res = server.getSessionService().hasJoinedServer(login.username(), serverId, addr);
                    if (res == null) {
                        disconnect(Component.translate("multiplayer.disconnect.unverified_username"));
                    } else {
                        this.conn = conn.withAuth();
                    }

                } catch (AuthenticationUnavailableException ex) {
                    disconnect(Component.translate("multiplayer.disconnect.authservers_down"));
                }
            } else {
                this.conn = conn.withAuth();
            }

            disconnect(Component.text("Encryption Success"));

        }
    }

    private void connectToBackend(ChannelHandlerContext ctx, Channel channel) {

        BackendConnection conn = new BackendConnection("localhost", 25566, channel, false);
        conn.connect().thenAccept(ch -> {
            ch.write(handshake);
            ch.write(login);
            ch.flush();
            setupForwarding(ctx, ch);
        });
    }

    private void disconnect(Component component) {

        channel.writeAndFlush(new ClientboundKickPacket(component)).addListener(ChannelFutureListener.CLOSE);

    }

    protected void setupEncryption(ChannelHandlerContext ctx, SecretKey key) {

        Cipher encrypt = CryptUtil.getCipher(Cipher.ENCRYPT_MODE, key);
        Cipher decrypt = CryptUtil.getCipher(Cipher.DECRYPT_MODE, key);

        ctx.pipeline().addBefore("splitter", "decrypt", new CryptDecoder(decrypt));
        ctx.pipeline().addBefore("prepender", "encrypt", new CryptEncoder(encrypt));
    }

    protected void setupForwarding(ChannelHandlerContext ctx, Channel forward) {

        ctx.pipeline().remove("splitter");
        ctx.pipeline().remove("decrypt");
        ctx.pipeline().remove("decoder");
        ctx.pipeline().remove("handler");
        ctx.pipeline().remove("prepender");
        ctx.pipeline().remove("encoder");
        ctx.pipeline().remove("encrypt");

        ctx.pipeline().addLast("forward", new PacketForwarder(forward));

    }

    public void changeState(ChannelHandlerContext ctx, PacketRegistry registry) {

        ctx.pipeline().remove("decoder");
        ctx.pipeline().addBefore(ctx.name(), "decoder", new PacketDecoder(registry));

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

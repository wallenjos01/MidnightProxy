package org.wallentines.mdproxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.netty.FrameEncoder;
import org.wallentines.mdproxy.netty.PacketEncoder;
import org.wallentines.mdproxy.netty.PacketForwarder;
import org.wallentines.mdproxy.packet.PacketRegistry;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;

public class BackendConnection {

    private final GameVersion version;
    private final String hostname;
    private final int port;
    private final int timeout;
    private final boolean haproxy;
    private Channel channel;

    public BackendConnection(GameVersion version, String host, int port, int timeout, boolean haproxy) {
        this.version = version;
        this.hostname = host;
        this.port = port;
        this.haproxy = haproxy;
        this.timeout = timeout;
    }

    public ChannelFuture connect(EventLoopGroup group) {

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast("frame_enc", new FrameEncoder())
                                .addLast("encoder", new PacketEncoder<>());

                        if(haproxy) {
                            ch.pipeline().addFirst(HAProxyMessageEncoder.INSTANCE);
                        }
                    }
                });


        ChannelFuture out = bootstrap.connect(hostname, port);
        this.channel = out.channel();

        return out;
    }

    public void setupForwarding(Channel client) {

        this.channel.config().setAutoRead(false);
        this.channel.pipeline().remove("frame_enc");
        this.channel.pipeline().remove("encoder");

        this.channel.pipeline().addLast(new PacketForwarder(client));
        this.channel.config().setAutoRead(true);
    }

    public void sendClientInformation(ClientConnectionImpl conn) {

        changePhase(ProtocolPhase.HANDSHAKE);
        channel.write(conn.handshakePacket(ServerboundHandshakePacket.Intent.LOGIN));

        changePhase(ProtocolPhase.LOGIN);
        channel.write(conn.loginPacket());

        channel.flush();

    }

    @SuppressWarnings("unchecked")
    public void changePhase(ProtocolPhase phase) {

        this.channel.pipeline().get(PacketEncoder.class).setRegistry(PacketRegistry.getServerbound(version, phase));
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isConnected() {
        return channel.isActive();
    }

}

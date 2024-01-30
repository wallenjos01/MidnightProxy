package org.wallentines.mdproxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.netty.*;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketRegistry;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;

public class BackendConnectionImpl implements BackendConnection {

    private final Backend backend;
    private final GameVersion version;
    private final int timeout;
    private final boolean haproxy;
    private Channel channel;
    private boolean forwarding = false;

    public BackendConnectionImpl(Backend backend, GameVersion version, int timeout, boolean haproxy) {
        this.backend = backend;
        this.version = version;
        this.haproxy = haproxy;
        this.timeout = timeout;
    }

    public ChannelFuture connect(EventLoopGroup group) {

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .option(ChannelOption.AUTO_READ, false)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast("frame_enc", new FrameEncoder())
                                .addLast("encoder", new PacketEncoder<>(PacketRegistry.HANDSHAKE));

                        if(haproxy) {
                            ch.pipeline().addFirst(HAProxyMessageEncoder.INSTANCE);
                        }
                    }
                });


        ChannelFuture out = bootstrap.connect(backend.hostname(), backend.port());
        this.channel = out.channel();

        return out;
    }

    public void setupForwarding(Channel client) {

        forwarding = true;

        this.channel.pipeline().remove("frame_enc");
        this.channel.pipeline().remove("encoder");

        this.channel.pipeline().addLast(new PacketForwarder(client));
        this.channel.config().setAutoRead(true);

    }

    public void setupStatus(StatusResponder res) {

        this.channel.pipeline().addLast("frame_dec", new FrameDecoder());
        this.channel.pipeline().addLast("decoder", new PacketDecoder<>(PacketRegistry.getClientbound(version, ProtocolPhase.STATUS)));

        this.channel.pipeline().addLast("handler", new PacketHandler<>(res));
        this.channel.config().setAutoRead(true);
    }

    public Backend getBackend() {
        return backend;
    }

    @SuppressWarnings("unchecked")
    public void changePhase(ProtocolPhase phase) {

        this.channel.pipeline().get(PacketEncoder.class).setRegistry(PacketRegistry.getServerbound(version, phase));
        this.channel.pipeline().get(PacketDecoder.class).setRegistry(PacketRegistry.getClientbound(version, phase));
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isConnected() {
        return channel.isActive();
    }

    @Override
    public boolean isForwarding() {
        return forwarding;
    }

    @Override
    public void send(Packet<ServerboundPacketHandler> packet) {
        if(channel.eventLoop().inEventLoop()) {
            channel.writeAndFlush(packet);
        } else {
            channel.eventLoop().submit(() -> {
                channel.writeAndFlush(packet);
            });
        }
    }


}

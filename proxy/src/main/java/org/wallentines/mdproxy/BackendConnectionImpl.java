package org.wallentines.mdproxy;

import io.netty.channel.*;
import org.jetbrains.annotations.Nullable;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.netty.*;
import org.wallentines.mdproxy.packet.*;

public class BackendConnectionImpl implements BackendConnection {

    private final Backend backend;
    private final GameVersion version;
    private final Channel channel;
    private boolean forwarding = false;

    public BackendConnectionImpl(Backend backend, GameVersion version, Channel channel) {
        this.backend = backend;
        this.version = version;
        this.channel = channel;
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
        PacketDecoder<ClientboundPacketHandler> dec = this.channel.pipeline().get(PacketDecoder.class);
        if(dec != null) dec.setRegistry(PacketRegistry.getClientbound(version, phase));
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
    public @Nullable String getBackendId(Proxy proxy) {
        return proxy.getBackends().getId(backend);
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

    public void close() {
        channel.close();
    }


}

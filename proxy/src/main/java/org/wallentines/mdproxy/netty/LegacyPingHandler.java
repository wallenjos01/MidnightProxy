package org.wallentines.mdproxy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.ClientConnectionImpl;
import org.wallentines.mdproxy.ProxyServer;
import org.wallentines.mdproxy.StatusEntry;
import org.wallentines.mdproxy.StatusMessage;
import org.wallentines.midnightlib.types.DefaultedSingleton;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * A packet handler for legacy ping requests. See <a href="https://wiki.vg/Server_List_Ping">...</a> for more information.
 */
public class LegacyPingHandler extends ChannelInboundHandlerAdapter {


    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyPingHandler.class);
    private static final String PING_CHANNEL = "MC|PingHost";

    private final ProxyServer server;
    private final DefaultedSingleton<InetSocketAddress> address;

    public LegacyPingHandler(ProxyServer server, DefaultedSingleton<InetSocketAddress> address) {
        this.server = server;
        this.address = address;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        boolean legacyPing = false;
        try {

            // All legacy pings start with FE
            if (buf.readUnsignedByte() != 0xFE) {
                return;
            }

            // Determine the ping version.
            // Pre-1.4 sends only the FE byte.
            if (!buf.isReadable()) {
                handleV0Request(ctx);
                legacyPing = true;
                return;
            }

            int payload = buf.readUnsignedByte();
            if (payload != 1) {
                return;
            }

            // 1.4-1.5 clients send only FE and the payload. 1.6 Also sends client information as a plugin message.
            ClientConnectionImpl conn;
            if (buf.isReadable()) {
                conn = readClientInfo(ctx, buf);
            } else {
                conn = new ClientConnectionImpl(ctx.channel(), address.get(), 60, ctx.channel().localAddress().toString(), 25565);
            }

            handleV1Request(ctx, conn);
            legacyPing = true;

        } catch (Exception ex) {

            LOGGER.error("An error occurred while handing a legacy ping!", ex);

        } finally {

            ctx.channel().pipeline().remove(this);

            if(!legacyPing) {
                buf.resetReaderIndex();
                ctx.fireChannelRead(buf);
            }

        }
    }

    private ClientConnectionImpl readClientInfo(ChannelHandlerContext ctx, ByteBuf buf) {

        int packetId = buf.readUnsignedByte();
        if(packetId != 0xFA) {
            throw new RuntimeException("Expected packet ID 0xFA, but found " + packetId);
        }

        short channelLength = buf.readShort();
        String channel = buf.readCharSequence(channelLength * 2, StandardCharsets.UTF_16BE).toString();
        if(!channel.equals(PING_CHANNEL)) {
            throw new RuntimeException("Expected plugin message in channel " + PING_CHANNEL + ", but found " + channel);
        }

        buf.readShort();
        int protocolVersion = buf.readUnsignedByte();

        short hostnameLength = buf.readShort();
        String hostname = buf.readCharSequence(hostnameLength, StandardCharsets.UTF_16BE).toString();

        int port = buf.readInt();

        return new ClientConnectionImpl(ctx.channel(), address.get(), protocolVersion, hostname, port);
    }

    private void handleV0Request(ChannelHandlerContext ctx) {

        LOGGER.debug("Received legacy ping (pre-1.4) from {}", ctx.channel().remoteAddress());

        ClientConnectionImpl conn = new ClientConnectionImpl(ctx.channel(), address.get(), 39, ctx.channel().localAddress().toString(), 25565);
        StatusEntry ent = conn.getStatusEntry(server);
        if(ent == null) {
            ctx.channel().close();
            return;
        }

        String data = String.format("%s§%d§%d",
                "",
                server.getOnlinePlayers(),
                server.getPlayerLimit()
        );

        kick(ctx, data);
    }

    private void handleV1Request(ChannelHandlerContext ctx, ClientConnectionImpl conn) {

        LOGGER.debug("Received legacy ping (1.4-1.6) from {}", ctx.channel().remoteAddress());

        StatusEntry ent = conn.getStatusEntry(server);
        if(ent == null) {
            ctx.channel().close();
            return;
        }
        StatusMessage msg = ent.create(new GameVersion("1.21", 767), server.getOnlinePlayers(), server.getPlayerLimit(), server.getIconCache());

        String data = String.format("§1\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d",
                msg.version().getProtocolVersion(),
                msg.version().getId(),
                msg.message().toLegacyText().replace("\u00A7", "\\u00A7"),
                msg.playersOnline(),
                msg.maxPlayers()
        );

        kick(ctx, data);
    }

    private void kick(ChannelHandlerContext ctx, String message) {

        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(0xFF);

        buf.writeShort(message.length());
        buf.writeCharSequence(message, StandardCharsets.UTF_16BE);

        ctx.writeAndFlush(buf).addListener(ChannelFutureListener.CLOSE);

    }

}

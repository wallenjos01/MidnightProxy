package org.wallentines.mdproxy.proxy;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.wallentines.mdproxy.util.CryptUtil;

import java.net.Proxy;
import java.security.KeyPair;

public class ProxyServer {

    private final EventLoopGroup bossLoopGroup;
    private final EventLoopGroup workerLoopGroup;
    private final ChannelGroup channelGroup;

    private final KeyPair keyPair;

    public ProxyServer() {

        this.bossLoopGroup = new NioEventLoopGroup();
        this.workerLoopGroup = new NioEventLoopGroup();

        this.channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        this.keyPair = CryptUtil.generateKeyPair();
    }

    public void startup(int port) throws Exception {

        MinecraftSessionService minecraft = new YggdrasilAuthenticationService(Proxy.NO_PROXY).createMinecraftSessionService();

        //srv.createMinecraftSessionService().hasJoinedServer(/* Username */, /* Server ID */, /* InetAddress */);

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossLoopGroup, workerLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {

                        ChannelPipeline p = socketChannel.pipeline();

                        p.addLast("splitter", new PacketSplitter());
                        p.addLast("decoder", new PacketDecoder());
                        p.addLast("handler", new ClientPacketHandler(minecraft, keyPair));
                        p.addLast("prepender", new LengthPrepender());
                        p.addLast("encoder", new PacketEncoder());

                    }
                });

        try {

            ChannelFuture conn = bootstrap.bind(port).sync();
            channelGroup.add(conn.channel());

        } catch (Exception ex) {
            shutdown();
            throw ex;
        }
    }

    public void shutdown() {
        channelGroup.close();
        bossLoopGroup.shutdownGracefully();
        workerLoopGroup.shutdownGracefully();
    }

}

package org.wallentines.mdproxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.lang.LocaleHolder;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.text.ComponentResolver;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.common.ClientboundKickPacket;
import org.wallentines.mdproxy.packet.config.ServerboundPluginMessagePacket;
import org.wallentines.mdproxy.packet.login.ClientboundLoginQueryPacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginPacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginQueryPacket;
import org.wallentines.midnightlib.event.HandlerList;
import org.wallentines.midnightlib.registry.Identifier;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

public class ClientConnectionImpl implements ClientConnection, LocaleHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger("ClientConnectionImpl");

    private final Channel channel;
    private final InetSocketAddress address;
    private final int protocolVersion;
    private final String hostname;
    private final int port;
    private PlayerInfo playerInfo;
    private boolean auth;
    private String locale;
    private BackendConnectionImpl backend;
    private final Map<Identifier, byte[]> cookies = new HashMap<>();
    private final Map<String, Queue<Task>> tasks = new HashMap<>();
    private final HandlerList<ServerboundPluginMessagePacket> pluginMessageEvent = new HandlerList<>();
    private final HandlerList<ServerboundLoginQueryPacket> loginQueryEvent = new HandlerList<>();

    private final Map<Integer, CompletableFuture<ServerboundLoginQueryPacket>> loginQueries = new HashMap<>();


    public ClientConnectionImpl(Channel channel, InetSocketAddress address, int protocolVersion, String hostname, int port) {
        this.channel = channel;
        this.address = address;
        this.protocolVersion = protocolVersion;
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public boolean playerInfoAvailable() {
        return playerInfo != null;
    }

    @Override
    public boolean authenticated() {
        return auth;
    }

    @Override
    public InetAddress address() {
        return address.getAddress();
    }

    public InetSocketAddress socketAddress() {
        return address;
    }

    @Override
    public TestResult bypassesPlayerLimit(Proxy server) {

        if(server.getPlayerLimit() == -1) {
            return TestResult.PASS;
        }
        if(!authenticated()) {
            return TestResult.NOT_ENOUGH_INFO;
        }

        return server.bypassesPlayerLimit(playerInfo) ? TestResult.PASS : TestResult.FAIL;
    }

    @Override
    public String hostname() {
        return hostname;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public int protocolVersion() {
        return protocolVersion;
    }

    @Override
    public String username() {
        return playerInfo == null ? channel.remoteAddress().toString() : playerInfo.username();
    }

    @Override
    public UUID uuid() {
        return playerInfo == null ? null : playerInfo.uuid();
    }

    @Override
    public PlayerInfo playerInfo() {
        return playerInfo;
    }

    @Override
    public byte[] getCookie(Identifier id) {
        return cookies.get(id);
    }

    @Override
    public String locale() {
        return locale;
    }

    @Override
    public ServerboundHandshakePacket handshakePacket(ServerboundHandshakePacket.Intent intent) {
        return new ServerboundHandshakePacket(protocolVersion, hostname, port, intent);
    }

    @Override
    public ServerboundLoginPacket loginPacket() {
        return new ServerboundLoginPacket(playerInfo.username(), playerInfo.uuid());
    }

    @Override
    public boolean hasDisconnected() {
        return !channel.isOpen();
    }

    @Override
    public BackendConnection getBackendConnection() {
        return backend;
    }

    public void setPlayerInfo(PlayerInfo playerInfo) {
        this.playerInfo = playerInfo;
    }

    public void setAuthenticated(boolean auth) {
        this.auth = auth;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setCookie(Identifier id, byte[] data) {
        this.cookies.put(id, data);
    }

    public void setBackend(BackendConnectionImpl backend) {
        this.backend = backend;
    }

    public void send(Packet<ClientboundPacketHandler> packet) {

        send(packet, null);
    }

    public void send(Packet<ClientboundPacketHandler> packet, ChannelFutureListener listener) {

        if(hasDisconnected()) {
            LOGGER.warn("Attempt to send packet to player {} after they disconnected!", username());
            return;
        }

        if(backend != null) {
            LOGGER.warn("Attempt to send packet to player {} after they connected to a backend!", username());
            return;
        }

        if(channel.eventLoop().inEventLoop()) {
            doSend(packet, listener);
        } else {
            channel.eventLoop().execute(() -> doSend(packet, listener));
        }
    }

    private void doSend(Packet<ClientboundPacketHandler> packet, ChannelFutureListener listener) {

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

    public void disconnect(Component component) {

        if(hasDisconnected()) {
            return;
        }

        Component cmp = ComponentResolver.resolveComponent(component, this);

        LOGGER.info("Disconnecting player {}: {}", username(), cmp.allText());

        if(backend == null) {
            send(new ClientboundKickPacket(cmp));
        }
        channel.close().awaitUninterruptibly();
    }

    public void disconnect() {
        if(hasDisconnected()) {
            return;
        }

        LOGGER.info("Disconnecting player {}", username());
        channel.close();
    }

    @Override
    public void registerTask(String taskQueue, Task task) {
        tasks.computeIfAbsent(taskQueue, k -> new ArrayDeque<>()).add(task);
    }

    @Override
    public void executeTasks(String taskQueue) {
        executeTasksAsync(taskQueue);
    }

    @Override
    public CompletableFuture<Void> executeTasksAsync(String taskQueue) {

        channel.config().setAutoRead(false);
        return CompletableFuture.runAsync(() -> {
            Queue<Task> toExecute = tasks.get(taskQueue);
            if(toExecute == null || toExecute.isEmpty()) return;

            List<Callable<Object>> toCall = new ArrayList<>(toExecute.size());
            while(!toExecute.isEmpty()) {
                Task task = toExecute.remove();
                toCall.add(Executors.callable(() -> {
                    try {
                        task.run(taskQueue, this);
                    } catch (Throwable th) {
                        LOGGER.error("An error occurred while running a task!", th);
                    }
                }));
            }

            try {
                channel.eventLoop().invokeAll(toCall);
            } catch (InterruptedException ex) {
                LOGGER.error("Unable to complete all tasks!", ex);
            }
            channel.config().setAutoRead(true);

        }, channel.eventLoop());
    }

    @Override
    public HandlerList<ServerboundPluginMessagePacket> pluginMessageEvent() {
        return pluginMessageEvent;
    }

    @Override
    public @Nullable ServerboundPluginMessagePacket awaitPluginMessage(Identifier id, int timeout) {

        CompletableFuture<ServerboundPluginMessagePacket> future = new CompletableFuture<>();

        pluginMessageEvent.register(future, msg -> {
            if(msg.channel().equals(id)) {
                future.complete(msg);
            }
        });

        ServerboundPluginMessagePacket out;
        try {
            out = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            out = null;
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.error("An exception occurred while awaiting a plugin message!", ex);
            out = null;
        }
        pluginMessageEvent.unregisterAll(future);
        return out;
    }

    public HandlerList<ServerboundLoginQueryPacket> loginQueryEvent() {
        return loginQueryEvent;
    }

    private void loginQueryReceived(ServerboundLoginQueryPacket packet) {

        CompletableFuture<ServerboundLoginQueryPacket> awaited = loginQueries.remove(packet.messageId());
        if(awaited == null) {
            LOGGER.warn("Received unsolicited login query in channel {}", packet.channel());
            return;
        }

        awaited.complete(packet);
    }

    @Override
    public CompletableFuture<ServerboundLoginQueryPacket> sendLoginQuery(Identifier id, ByteBuf data) {

        ClientboundLoginQueryPacket pck = new ClientboundLoginQueryPacket(id, data);
        CompletableFuture<ServerboundLoginQueryPacket> out = new CompletableFuture<>();
        loginQueries.put(pck.messageId(), out);

        send(pck);
        return out;
    }

    @Override
    public ServerboundLoginQueryPacket awaitLoginQuery(Identifier id, ByteBuf data, int timeout) {

        CompletableFuture<ServerboundLoginQueryPacket> future = sendLoginQuery(id, data);
        ServerboundLoginQueryPacket out;
        try {
            out = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            out = null;
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.error("An exception occurred while awaiting a login query!", ex);
            out = null;
        }
        loginQueryEvent.unregisterAll(future);
        return out;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public String getLanguage() {
        return locale == null ? "en_us" : locale;
    }
}

package org.wallentines.mdproxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.lang.LocaleHolder;
import org.wallentines.mcore.lang.UnresolvedComponent;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdcfg.Tuples;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.common.ClientboundKickPacket;
import org.wallentines.mdproxy.packet.common.ServerboundPluginMessagePacket;
import org.wallentines.mdproxy.packet.common.ServerboundResourcePackStatusPacket;
import org.wallentines.mdproxy.packet.login.ClientboundLoginQueryPacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginQueryPacket;
import org.wallentines.midnightlib.event.ConcurrentHandlerList;
import org.wallentines.midnightlib.event.HandlerList;
import org.wallentines.midnightlib.registry.Identifier;

import java.io.IOException;
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


    private PlayerProfile profile;
    private boolean auth;
    private String locale;
    private BackendConnectionImpl backend;

    private boolean disconnected = false;

    ProtocolPhase phase = ProtocolPhase.HANDSHAKE;
    boolean wasReconnected = false;

    private final Map<Identifier, byte[]> cookies = new HashMap<>();

    @Deprecated
    private final Map<String, Queue<Task>> tasks = new HashMap<>();

    private final Map<Integer, CompletableFuture<ServerboundLoginQueryPacket>> loginQueries = new HashMap<>();
    private final Map<UUID, CompletableFuture<ServerboundResourcePackStatusPacket>> resourcePacks = new HashMap<>();

    private final HandlerList<ServerboundPluginMessagePacket> pluginMessageEvent = new HandlerList<>();
    private final HandlerList<ServerboundLoginQueryPacket> loginQueryEvent = new HandlerList<>();


    private final ConcurrentHandlerList<ClientConnection> preLoginEvent;
    private final ConcurrentHandlerList<ClientConnection> postLoginEvent;
    private final ConcurrentHandlerList<Tuples.T2<Backend, ClientConnection>> enterConfigurationEvent;
    private final ConcurrentHandlerList<Tuples.T2<Backend, ClientConnection>> preConnectBackendEvent;
    private final ConcurrentHandlerList<Tuples.T2<Backend, ClientConnection>> postConnectBackendEvent;

    private final ServerboundHandshakePacket.Intent intent;


    public ClientConnectionImpl(Channel channel, InetSocketAddress address, int protocolVersion, String hostname, int port, ServerboundHandshakePacket.Intent intent) {
        this.channel = channel;
        this.address = address;
        this.protocolVersion = protocolVersion;
        this.hostname = hostname;
        this.port = port;
        this.intent = intent;

        //ThreadPoolExecutor svc = new ThreadPoolExecutor(1, 4, 5000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        ExecutorService svc = channel.parent().eventLoop();

        this.preLoginEvent = new ConcurrentHandlerList<>(svc);
        this.postLoginEvent = new ConcurrentHandlerList<>(svc);
        this.enterConfigurationEvent = new ConcurrentHandlerList<>(svc);
        this.preConnectBackendEvent = new ConcurrentHandlerList<>(svc);
        this.postConnectBackendEvent = new ConcurrentHandlerList<>(svc);

        loginQueryEvent.register(this, this::loginQueryReceived);
    }

    @Deprecated
    @Override
    public boolean playerInfoAvailable() {
        return profile != null;
    }


    @Override
    public boolean profileAvailable() {
        return profile != null;
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

        return server.bypassesPlayerLimit(profile) ? TestResult.PASS : TestResult.FAIL;
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
        return profile == null ? channel.remoteAddress().toString() : profile.username();
    }

    @Override
    public UUID uuid() {
        return profile == null ? null : profile.uuid();
    }

    @Deprecated
    @Override
    public PlayerInfo playerInfo() {
        return new PlayerInfo(profile.username(), profile.uuid());
    }

    public PlayerProfile profile() {
        return profile;
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
    public boolean hasDisconnected() {
        return disconnected;
    }

    @Override
    public BackendConnectionImpl getBackendConnection() {
        return backend;
    }

    @Deprecated
    public void setPlayerInfo(PlayerInfo profile) {
        this.profile = new PlayerProfile(profile.uuid(), profile.username());
    }

    public void setProfile(PlayerProfile profile) {
        this.profile = profile;
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
        this.cookies.clear();
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

    public void disconnect(UnresolvedComponent component) {
        disconnect(component.resolveFor(this));
    }


    public void disconnect() {
        disconnect(null, true);
    }

    public void disconnect(Component component) {
        disconnect(component, true);
    }

    public void disconnect(Component component, boolean log) {

        if(hasDisconnected()) {
            return;
        }
        cleanup();

        if(!channel.isActive()) {
            return;
        }

        if(component == null) {
            LOGGER.info("Disconnecting player {}", username());
        } else {
            if(log) LOGGER.info("Disconnecting player {}: {}", username(), component.allText());
            if(backend == null) {
                send(new ClientboundKickPacket(component));
            }
        }

        disconnected = true;
        channel.close().awaitUninterruptibly();
    }

    public void cleanup() {
        for(CompletableFuture<ServerboundLoginQueryPacket> query : loginQueries.values()) {
            query.completeExceptionally(new IOException("Client disconnected"));
        }
        for(CompletableFuture<ServerboundResourcePackStatusPacket> packStatus : resourcePacks.values()) {
            packStatus.completeExceptionally(new IOException("Client disconnected"));
        }
    }

    @Deprecated
    @Override
    public void registerTask(String taskQueue, Task task) {

        switch (taskQueue) {
            case Task.PRE_LOGIN_QUEUE -> preLoginEvent.register(this, conn -> task.run(taskQueue, conn));
            case Task.POST_LOGIN_QUEUE -> postLoginEvent.register(this, conn -> task.run(taskQueue, conn));
            case Task.CONFIGURE_QUEUE -> enterConfigurationEvent.register(this, conn -> task.run(taskQueue, conn.p2));
            case Task.PRE_BACKEND_CONNECT -> preConnectBackendEvent.register(this, conn -> task.run(taskQueue, conn.p2));
            case Task.POST_BACKEND_CONNECT -> postConnectBackendEvent.register(this, conn -> task.run(taskQueue, conn.p2));
            default -> tasks.computeIfAbsent(taskQueue, k -> new ArrayDeque<>()).add(task);
        }

    }

    @Deprecated
    @Override
    public void executeTasks(String taskQueue) {
        executeTasksAsync(taskQueue).join();
    }

    @Deprecated
    @Override
    public CompletableFuture<Void> executeTasksAsync(String taskQueue) {

        return CompletableFuture.runAsync(() -> {

            Queue<Task> toExecute = tasks.get(taskQueue);

            if(toExecute == null || toExecute.isEmpty()) {
                return;
            }

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
                channel.eventLoop().parent().invokeAll(toCall);
            } catch (InterruptedException ex) {
                LOGGER.error("Unable to complete all tasks!", ex);
            }

        }, channel.eventLoop().parent());
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
            LOGGER.warn("Received unsolicited login query #{}", packet.messageId());
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

    @Override
    public boolean wasReconnected() {
        return wasReconnected;
    }

    @Override
    public CompletableFuture<ServerboundResourcePackStatusPacket> sendResourcePack(ResourcePack pack) {

        if(phase == ProtocolPhase.LOGIN || phase == ProtocolPhase.HANDSHAKE) {
            return CompletableFuture.failedFuture(new IllegalStateException("Resource packs cannot be applied during " + phase.name()));
        }

        if(resourcePacks.containsKey(pack.uuid())) {
            return CompletableFuture.failedFuture(new IllegalStateException("Attempt to apply the same resource pack twice`!"));
        }

        LOGGER.warn("Sent resource pack push packet: {}", pack);

        CompletableFuture<ServerboundResourcePackStatusPacket> out = new CompletableFuture<>();
        resourcePacks.put(pack.uuid(), out);

        send(pack.toPacket());

        return out;
    }

    @Override
    public ServerboundHandshakePacket.Intent getIntent() {
        return intent;
    }

    void onPackResponse(ServerboundResourcePackStatusPacket packet) {

        if(packet.action() == ServerboundResourcePackStatusPacket.Action.ACCEPTED || packet.action() == ServerboundResourcePackStatusPacket.Action.DOWNLOAD_COMPLETE) {
            // Expect more packets
            return;
        }

        CompletableFuture<ServerboundResourcePackStatusPacket> future = resourcePacks.remove(packet.packId());
        if(future == null) {
            LOGGER.warn("Received unsolicited resource pack response for pack {}", packet.packId());
            return;
        }

        future.complete(packet);
    }

    @Override
    public ConcurrentHandlerList<ClientConnection> preLoginEvent() {
        return preLoginEvent;
    }

    @Override
    public ConcurrentHandlerList<ClientConnection> postLoginEvent() {
        return postLoginEvent;
    }

    @Override
    public ConcurrentHandlerList<Tuples.T2<Backend, ClientConnection>> enterConfigurationEvent() {
        return enterConfigurationEvent;
    }

    @Override
    public ConcurrentHandlerList<Tuples.T2<Backend, ClientConnection>> preConnectBackendEvent() {
        return preConnectBackendEvent;
    }

    @Override
    public ConcurrentHandlerList<Tuples.T2<Backend, ClientConnection>> postConnectBackendEvent() {
        return postConnectBackendEvent;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public String getLanguage() {
        return locale == null ? "en_us" : locale;
    }

    @Nullable
    public StatusEntry getStatusEntry(ProxyServer server) {

        PriorityQueue<StatusEntry> ent = new PriorityQueue<>(server.getStatusEntries());
        for (StatusEntry e : ent) {
            if (e.canUse(new ConnectionContext(this, server))) {
                return e;
            }
        }
        return null;
    }
}

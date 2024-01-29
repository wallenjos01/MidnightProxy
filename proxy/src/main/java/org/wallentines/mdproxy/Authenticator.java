package org.wallentines.mdproxy;

import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import org.wallentines.mcore.text.Component;

import java.net.InetAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Authenticator {

    private final ThreadPoolExecutor executor;
    private final MinecraftSessionService service;

    public Authenticator(MinecraftSessionService service, int maxThreads) {
        this.service = service;
        this.executor = new ThreadPoolExecutor(1, maxThreads, 5000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(maxThreads));
    }

    public CompletableFuture<ProfileResult> authenticate(ClientPacketHandler handler, String username, String serverId, InetAddress address) {

        return CompletableFuture.supplyAsync(() -> {
            try {

                ProfileResult res = service.hasJoinedServer(username, serverId, address);
                if (res == null) {
                    handler.disconnect(Component.translate("multiplayer.disconnect.unverified_username"));
                    return null;
                }

                return res;

            } catch (AuthenticationUnavailableException ex) {
                handler.disconnect(Component.translate("multiplayer.disconnect.authservers_down"));
                return null;
            }
        }, executor);

    }

}

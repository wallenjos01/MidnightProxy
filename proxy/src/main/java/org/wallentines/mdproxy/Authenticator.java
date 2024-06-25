package org.wallentines.mdproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.DecodeException;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.serializer.SerializeResult;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger("Authenticator");
    private static final String SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined";
    private final ThreadPoolExecutor executor;
    private final ProxyServer server;

    public Authenticator(ProxyServer server, int maxThreads) {
        this.server = server;
        this.executor = new ThreadPoolExecutor(1, maxThreads, 5000, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
    }

    public CompletableFuture<PlayerProfile> authenticate(ClientConnection connection, String serverId) {

        if(!connection.playerInfoAvailable()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {

            StringBuilder url = new StringBuilder(SESSION_URL)
                    .append("?username=").append(connection.username())
                    .append("&serverId=").append(serverId);

            if(server.preventProxyConnections()) {
                url.append("&ip=").append(connection.address().getHostAddress());
            }

            try {
                ConfigSection response = makeHttpRequest(new URL(url.toString()));
                SerializeResult<PlayerProfile> res = PlayerProfile.SERIALIZER.deserialize(ConfigContext.INSTANCE, response);
                if(res.isComplete()) {
                    return res.getOrThrow();
                }

                LOGGER.warn("Unable to parse authentication response! {}", res.getError());
                return null;

            } catch (IOException ex) {
                return null;
            }

        }, executor);

    }


    private static ConfigSection makeHttpRequest(URL url) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();

        if(conn.getResponseCode() != 200) {
            LOGGER.warn("Received invalid response from {}", url);
            return null;
        }

        InputStream responseStream = conn.getInputStream();
        ConfigObject obj;
        try {
            obj = JSONCodec.minified().decode(ConfigContext.INSTANCE, responseStream);
        } catch (DecodeException ex) {

            LOGGER.warn("Unable to parse response from {}!", url, ex);
            return new ConfigSection();
        }
        if(!obj.isSection()) {

            LOGGER.warn("Received non-section response from {}! {}", url, obj);
            return new ConfigSection();
        }

        ConfigSection out = obj.asSection();

        responseStream.close();
        conn.disconnect();

        return out;
    }

    public void close() {
        executor.shutdown();
    }

}

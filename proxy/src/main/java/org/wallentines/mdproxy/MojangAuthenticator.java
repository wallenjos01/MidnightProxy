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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class MojangAuthenticator implements Authenticator {

    private static final Pattern PATTERN = Pattern.compile("[A-Za-z0-9_]+");
    private static final Logger LOGGER = LoggerFactory.getLogger("MojangAuthenticator");
    private static final String SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined";

    public static final Type TYPE = MojangAuthenticator::new;

    private final Proxy server;

    public MojangAuthenticator(Proxy server) {
        this.server = server;
    }

    @Override
    public Type getType() {
        return TYPE;
    }

    @Override
    public boolean canAuthenticate(ClientConnection connection) {
        return true;
    }

    public PlayerProfile authenticate(ClientConnection connection, String serverId) {

        if(!connection.playerInfoAvailable()) {
            return null;
        }

        String username = connection.username();
        if(username.isEmpty() || username.length() > 16 || !PATTERN.matcher(username).matches()) {
            return null;
        }

        StringBuilder url = new StringBuilder(SESSION_URL)
                .append("?username=").append(connection.username())
                .append("&serverId=").append(URLEncoder.encode(serverId, StandardCharsets.UTF_8));

        if(server.preventProxyConnections()) {
            url.append("&ip=").append(URLEncoder.encode(connection.address().getHostAddress(), StandardCharsets.UTF_8));
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
    }

    @Override
    public boolean shouldClientAuthenticate() {
        return true;
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

}

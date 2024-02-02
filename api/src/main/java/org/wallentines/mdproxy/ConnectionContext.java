package org.wallentines.mdproxy;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConnectionContext {

    private final ClientConnection connection;
    private final Map<String, String> meta;

    public ConnectionContext(ClientConnection connection) {
        this.connection = connection;
        this.meta = new HashMap<>();
    }

    public ClientConnection getConnection() {
        return connection;
    }

    public String getMetaProperty(String key) {
        return meta.get(key);
    }

    public String setMetaProperty(String key, String value) {
        return meta.put(key, value);
    }

    public String hostname() {
        return connection.hostname();
    }

    public int port() {
        return connection.port();
    }

    public InetAddress address() {
        return connection.address();
    }

    public String username() {
        return connection.username();
    }

    public UUID uuid() {
        return connection.uuid();
    }

}

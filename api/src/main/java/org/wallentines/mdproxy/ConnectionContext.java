package org.wallentines.mdproxy;


import org.wallentines.pseudonym.PipelineContext;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConnectionContext {

    private final ClientConnection connection;
    private final Proxy proxy;
    private final Map<String, String> meta;

    public ConnectionContext(ClientConnection connection, Proxy proxy) {
        this.connection = connection;
        this.proxy = proxy;
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

    public PipelineContext toPipelineContext() {
        PipelineContext.Builder builder = PipelineContext.builder();
        builder.add(connection);
        for(String s : meta.keySet()) {
            builder.withContextPlaceholder(s, meta.get(s));
        }
        return builder.build();
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

    public String addressString() { return connection.address().toString(); }

    public String username() {
        return connection.username();
    }

    public UUID uuid() {
        return connection.uuid();
    }

    public String uuidString() {
        return connection.uuid().toString();
    }

    public String locale() {
        return connection.locale();
    }

    public Proxy getProxy() {
        return proxy;
    }
}

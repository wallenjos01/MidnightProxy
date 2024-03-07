package org.wallentines.mdproxy.whitelist;

import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ClientConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Whitelist {

    private List<UUID> uuids = new ArrayList<>();
    private List<String> usernames = new ArrayList<>();

    public boolean isWhitelisted(ClientConnection conn) {
        return uuids.contains(conn.uuid()) || usernames.contains(conn.username());
    }

    public Whitelist(Collection<UUID> uuids, Collection<String> usernames) {
        if(uuids != null) {
            this.uuids.addAll(uuids);
        }
        if(usernames != null) {
            this.usernames.addAll(usernames);
        }
    }

    public static final Serializer<Whitelist> SERIALIZER = ObjectSerializer.create(
            Serializer.UUID.listOf().<Whitelist>entry("uuids", wl -> wl.uuids).optional(),
            Serializer.STRING.listOf().<Whitelist>entry("names", wl -> wl.usernames).optional(),
            Whitelist::new
    );


}

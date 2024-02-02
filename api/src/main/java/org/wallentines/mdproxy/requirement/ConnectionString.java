package org.wallentines.mdproxy.requirement;

import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ClientConnection;
import org.wallentines.midnightlib.requirement.StringCheck;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

public class ConnectionString extends ConnectionCheck {

    private final Function<ClientConnection, String> getter;
    private final Set<String> values;
    public ConnectionString(Function<ClientConnection, String> getter, Collection<String> coll, boolean requireAuth, boolean requireLocale) {
        super(requireAuth, false, requireLocale, null);
        this.getter = getter;
        this.values = Set.copyOf(coll);
    }

    @Override
    public boolean test(ClientConnection conn) {
        return values.contains(getter.apply(conn));
    }

    public static Serializer<ConnectionString> serializer(Function<ClientConnection, String> getter, boolean requireAuth, boolean requireLocale) {
        return StringCheck.serializer(as -> as.values, values -> new ConnectionString(getter, values, requireAuth, requireLocale));
    }
}

package org.wallentines.mdproxy.requirement;

import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.requirement.StringCheck;

import java.util.Collection;
import java.util.Set;

public class Cookie extends ConnectionCheck {
    private final Identifier cookie;
    private final Set<String> values;

    public Cookie(Identifier cookie, Collection<String> values) {
        super(true, Set.of(cookie));
        this.cookie = cookie;
        this.values = Set.copyOf(values);
    }

    public static final Serializer<Cookie> SERIALIZER = ObjectSerializer.create(
            Identifier.serializer("minecraft").entry("cookie", c -> c.cookie),
            StringCheck.STRING_SERIALIZER.entry("value", c -> c.values),
            Cookie::new
    );

    @Override
    public boolean test(ConnectionContext conn) {
        byte[] c = conn.getConnection().getCookie(cookie);
        String str = c == null ? "" : new String(c);
        return values.contains(str);
    }
}

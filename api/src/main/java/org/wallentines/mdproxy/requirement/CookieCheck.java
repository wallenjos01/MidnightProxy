package org.wallentines.mdproxy.requirement;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.requirement.StringCheck;

import java.util.Collection;
import java.util.Set;

public class CookieCheck implements ConnectionCheck {
    private final Identifier cookie;
    private final Set<String> values;

    public CookieCheck(Identifier cookie, Collection<String> values) {
        this.cookie = cookie;
        this.values = Set.copyOf(values);
    }

    @Override
    public boolean requiresAuth() {
        return true;
    }

    @Override
    public @NotNull Collection<Identifier> getRequiredCookies() {
        return Set.of(cookie);
    }

    @Override
    public boolean check(ConnectionContext ctx) {
        byte[] c = ctx.getConnection().getCookie(cookie);
        String str = c == null ? "" : new String(c);
        return values.contains(str);
    }

    @Override
    public <O> SerializeResult<O> serialize(SerializeContext<O> ctx) {
        return SERIALIZER.serialize(ctx, this);
    }

    public static final Serializer<CookieCheck> SERIALIZER = ObjectSerializer.create(
            Identifier.serializer("minecraft").entry("cookie", c -> c.cookie),
            StringCheck.STRING_SERIALIZER.entry("value", c -> c.values),
            CookieCheck::new
    );

    public static final ConnectionCheckType TYPE = new ConnectionCheckType() {
        @Override
        protected <O> SerializeResult<ConnectionCheck> deserializeCheck(SerializeContext<O> ctx, O value) {
            return SERIALIZER.deserialize(ctx, value).flatMap(cook -> cook);
        }
    };
}

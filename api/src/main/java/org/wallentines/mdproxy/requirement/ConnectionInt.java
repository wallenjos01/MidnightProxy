package org.wallentines.mdproxy.requirement;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.math.Range;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.requirement.Check;
import org.wallentines.midnightlib.requirement.NumberCheck;
import org.wallentines.midnightlib.requirement.StringCheck;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class ConnectionInt extends NumberCheck<ConnectionContext, Integer> implements ConnectionCheck{

    private final boolean requireAuth;

    public ConnectionInt(Function<ConnectionContext, Integer> getter, Range<Integer> valid, boolean requireAuth) {
        super(Range.INTEGER, getter, valid);
        this.requireAuth = requireAuth;
    }

    @Override
    public boolean requiresAuth() {
        return requireAuth;
    }

    @Override
    public @NotNull Collection<Identifier> getRequiredCookies() {
        return Collections.emptyList();
    }

    public static ConnectionCheckType type(Function<ConnectionContext, Integer> getter, boolean requireAuth) {
        return new ConnectionCheckType() {
            @Override
            protected <O> SerializeResult<ConnectionCheck> deserializeCheck(SerializeContext<O> ctx, O value) {
                return Range.INTEGER.fieldOf("value").deserialize(ctx, value).flatMap(str -> new ConnectionInt(getter, str, requireAuth));
            }
        };
    }
}

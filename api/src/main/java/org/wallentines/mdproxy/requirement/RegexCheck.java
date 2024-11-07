package org.wallentines.mdproxy.requirement;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

public class RegexCheck implements ConnectionCheck {

    private final Function<ConnectionContext, String> getter;
    private final Pattern pattern;
    private final boolean requireAuth;

    public RegexCheck(Function<ConnectionContext, String> getter, Pattern pattern, boolean requireAuth) {
        this.getter = getter;
        this.pattern = pattern;
        this.requireAuth = requireAuth;
    }

    @Override
    public boolean requiresAuth() {
        return requireAuth;
    }

    @Override
    public @NotNull Collection<Identifier> getRequiredCookies() {
        return List.of();
    }

    @Override
    public boolean check(ConnectionContext data) {
        return pattern.matcher(getter.apply(data)).matches();
    }

    @Override
    public <O> SerializeResult<O> serialize(SerializeContext<O> context) {
        return SerializeResult.ofNullable(context.toString(pattern.pattern()));
    }

    public static ConnectionCheckType type(Function<ConnectionContext, String> getter, boolean requireAuth) {
        return new ConnectionCheckType() {
            @Override
            protected <O> SerializeResult<ConnectionCheck> deserializeCheck(SerializeContext<O> ctx, O value) {
                return Serializer.STRING.fieldOf("value").deserialize(ctx, value).flatMap(str -> new RegexCheck(getter, Pattern.compile(str), requireAuth));
            }
        };
    }

}

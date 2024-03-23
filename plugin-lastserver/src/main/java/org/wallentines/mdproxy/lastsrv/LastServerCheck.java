package org.wallentines.mdproxy.lastsrv;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.mdproxy.requirement.ConnectionCheck;
import org.wallentines.mdproxy.requirement.ConnectionCheckType;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.Collection;
import java.util.Collections;

public class LastServerCheck implements ConnectionCheck {

    private final boolean requireAuth;

    public LastServerCheck(boolean requireAuth) {
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

    @Override
    public boolean check(ConnectionContext ctx) {

        LastServerPlugin pl = ctx.getProxy().getPluginManager().get(LastServerPlugin.class);
        if(pl == null) {
            LastServerPlugin.LOGGER.warn("Attempt to use last_server check when plugin is disabled!");
            return false;
        }

        String data = pl.getDataManager().getData(ctx.getConnection().uuid().toString()).getOrDefault("last_server", (String) null);
        if(data == null) return false;

        ctx.setMetaProperty("last_server.backend", data);
        return true;
    }

    @Override
    public <O> SerializeResult<O> serialize(SerializeContext<O> ctx) {
        return SERIALIZER.serialize(ctx, this);
    }

    private static final Serializer<LastServerCheck> SERIALIZER = ObjectSerializer.create(
            Serializer.BOOLEAN.entry("require_auth", LastServerCheck::requiresAuth).orElse(true),
            LastServerCheck::new
    );

    public static final ConnectionCheckType TYPE = new ConnectionCheckType() {
        @Override
        protected <O> SerializeResult<ConnectionCheck> deserializeCheck(SerializeContext<O> ctx, O value) {
            return SERIALIZER.deserialize(ctx, value).flatMap(o -> o);
        }
    };
}

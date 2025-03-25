package org.wallentines.mdproxy.lastsrv;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.mdproxy.DataManager;
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

        DataManager dm = pl.getDataManager();
        synchronized (dm) {
            String data = dm.getData(ctx.getConnection().uuid().toString()).getOrDefault("last_server", (String) null);
            if (data == null) return false;

            ctx.setMetaProperty("last_server.backend", data);
            return true;
        }
    }

    @Override
    public Type type() {
        return TYPE;
    }

    private static final Serializer<LastServerCheck> SERIALIZER = ObjectSerializer.create(
            Serializer.BOOLEAN.entry("require_auth", LastServerCheck::requiresAuth).orElse(true),
            LastServerCheck::new
    );

    public static class Type implements ConnectionCheckType<LastServerCheck> {

        @Override
        public Serializer<LastServerCheck> serializer() {
            return SERIALIZER;
        }
    }

    public static final Type TYPE = new Type();
}

package org.wallentines.mdproxy.lastsrv;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.registry.Identifier;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdcfg.sql.Condition;
import org.wallentines.mdcfg.sql.DataType;
import org.wallentines.mdcfg.sql.DataValue;
import org.wallentines.mdcfg.sql.QueryResult;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.mdproxy.DataManager;
import org.wallentines.mdproxy.requirement.ConnectionCheck;
import org.wallentines.mdproxy.requirement.ConnectionCheckType;
import org.wallentines.mdproxy.sql.SQLPlugin;

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

        LastServerPlugin pl =
            ctx.getProxy().getPluginManager().get(LastServerPlugin.class);
        if (pl == null) {
            LastServerPlugin.LOGGER.warn(
                "Attempt to use last_server check when plugin is disabled!");
            return false;
        }

        AtomicBoolean result = new AtomicBoolean(false);

        pl.connectDatabase(ctx.getProxy())
            .thenAccept(sql -> {
                UUID uid = ctx.uuid();
                ByteBuffer uuidBuf = ByteBuffer.allocate(16);
                uuidBuf.putLong(0, uid.getMostSignificantBits());
                uuidBuf.putLong(8, uid.getLeastSignificantBits());
                DataValue dv = DataType.BLOB.create(uuidBuf);

                QueryResult res = sql.select(Tables.TABLE_NAME)
                                      .where(Condition.equals("player", dv))
                                      .execute();

                if (res.rows() > 0) {
                    String id = res.get(0).getString("server");
                    ctx.setMetaProperty("last_server_backend", id);
                    result.set(true);
                }
            })
            .join();

        return result.get();
    }

    @Override
    public Type type() {
        return TYPE;
    }

    private static final Serializer<LastServerCheck> SERIALIZER =
        ObjectSerializer.create(
            Serializer.BOOLEAN
                .entry("require_auth", LastServerCheck::requiresAuth)
                .orElse(true),
            LastServerCheck::new);

    public static class Type implements ConnectionCheckType<LastServerCheck> {

        @Override
        public Serializer<LastServerCheck> serializer() {
            return SERIALIZER;
        }
    }

    public static final Type TYPE = new Type();
}

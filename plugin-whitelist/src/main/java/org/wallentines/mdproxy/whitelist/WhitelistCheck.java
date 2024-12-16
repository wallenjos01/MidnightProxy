package org.wallentines.mdproxy.whitelist;

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

public class WhitelistCheck implements ConnectionCheck {

    private final boolean requireAuth;
    private final String listId;
    private Whitelist whitelist;

    public WhitelistCheck(boolean requireAuth, String whitelist) {
        this.requireAuth = requireAuth;
        this.listId = whitelist;
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
    public boolean check(ConnectionContext conn) {

        if(whitelist == null || whitelist.isInvalid()) {
            whitelist = conn.getProxy().getPluginManager().get(WhitelistPlugin.class).getLists().get(listId);
            if(whitelist == null) return false;
        }

        return whitelist.isWhitelisted(conn.getConnection());
    }

    @Override
    public <O> SerializeResult<O> serialize(SerializeContext<O> ctx) {
        return SERIALIZER.serialize(ctx, this);
    }

    public static final ConnectionCheckType TYPE = new ConnectionCheckType() {
        @Override
        protected <O> SerializeResult<ConnectionCheck> deserializeCheck(SerializeContext<O> ctx, O value) {
            return SERIALIZER.deserialize(ctx, value).flatMap(w -> w);
        }
    };

    private static final Serializer<WhitelistCheck> SERIALIZER = ObjectSerializer.create(
            Serializer.BOOLEAN.<WhitelistCheck>entry("require_auth", chk -> chk.requireAuth).orElse(true),
            Serializer.STRING.entry("whitelist", chk -> chk.listId),
            WhitelistCheck::new
    );

}

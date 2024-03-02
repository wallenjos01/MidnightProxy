package org.wallentines.mdproxy.requirement;


import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.math.Range;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.requirement.CompositeCheck;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Composite extends CompositeCheck<ConnectionContext, ConnectionCheckType, ConnectionRequirement> implements ConnectionCheck {

    public Composite(Serializer<ConnectionRequirement> general, Range<Integer> range, Collection<ConnectionRequirement> values) {
        super(general, range, values);
    }
    @Override
    public boolean requiresAuth() {
        for(ConnectionRequirement r : requirements) {
            if(r.requiresAuth()) return true;
        }
        return false;
    }
    @Override
    public @NotNull Collection<Identifier> getRequiredCookies() {
        Set<Identifier> out = new HashSet<>();
        for(ConnectionRequirement r : requirements) {
            Collection<Identifier> cookies = r.getRequiredCookies();
            if(cookies != null) out.addAll(cookies);
        }
        return out;
    }

    public static final ConnectionCheckType TYPE = new ConnectionCheckType() {
        @Override
        protected <O> SerializeResult<ConnectionCheck> deserializeCheck(SerializeContext<O> ctx, O value) {
            return CompositeCheck.serializer(ConnectionRequirement.SERIALIZER, Composite::new).deserialize(ctx, value).flatMap(cmp -> cmp);
        }
    };

}

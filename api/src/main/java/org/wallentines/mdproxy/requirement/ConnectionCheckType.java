package org.wallentines.mdproxy.requirement;

import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightlib.requirement.Check;
import org.wallentines.midnightlib.requirement.CheckType;

public abstract class ConnectionCheckType implements CheckType<ConnectionContext> {

    @Override
    public final <O> SerializeResult<Check<ConnectionContext>> deserialize(SerializeContext<O> ctx, O value) {
        return deserializeCheck(ctx, value).flatMap(cc -> cc);
    }

    protected abstract <O> SerializeResult<ConnectionCheck> deserializeCheck(SerializeContext<O> ctx, O value);


    public static final Registry<ConnectionCheckType> REGISTRY = new Registry<>("mdp");
    public static final ConnectionCheckType HOSTNAME = REGISTRY.register("hostname", ConnectionString.type(ConnectionContext::hostname, false));
    public static final ConnectionCheckType PORT = REGISTRY.register("port", ConnectionInt.type(ConnectionContext::port, false));
    public static final ConnectionCheckType ADDRESS = REGISTRY.register("ip_address", ConnectionString.type(ConnectionContext::addressString, false));
    public static final ConnectionCheckType USERNAME = REGISTRY.register("username", ConnectionString.type(ConnectionContext::username, true));
    public static final ConnectionCheckType UUID = REGISTRY.register("uuid", ConnectionString.type(ConnectionContext::uuidString, true));
    public static final ConnectionCheckType LOCALE = REGISTRY.register("locale", ConnectionString.type(ConnectionContext::locale, true));
    public static final ConnectionCheckType COOKIE = REGISTRY.register("cookie", Cookie.TYPE);
    public static final ConnectionCheckType COMPOSITE = REGISTRY.register("composite", Composite.TYPE);

}

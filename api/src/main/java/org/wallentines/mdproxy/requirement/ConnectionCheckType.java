package org.wallentines.mdproxy.requirement;

import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightlib.requirement.Check;
import org.wallentines.midnightlib.requirement.CheckType;

public abstract class ConnectionCheckType implements CheckType<ConnectionContext> {

    @Override
    public final <O> SerializeResult<Check<ConnectionContext>> deserialize(SerializeContext<O> ctx, O value) {
        return deserializeCheck(ctx, value).flatMap(cc -> cc);
    }

    protected abstract <O> SerializeResult<ConnectionCheck> deserializeCheck(SerializeContext<O> ctx, O value);


    public static final Registry<Identifier, ConnectionCheckType> REGISTRY = Registry.create("mdp");
    public static final ConnectionCheckType DATE = REGISTRY.tryRegister("date", DateCheck.TYPE);
    public static final ConnectionCheckType HOSTNAME = REGISTRY.tryRegister("hostname", ConnectionString.type(ConnectionContext::hostname, false));
    public static final ConnectionCheckType PORT = REGISTRY.tryRegister("port", ConnectionInt.type(ConnectionContext::port, false));
    public static final ConnectionCheckType ADDRESS = REGISTRY.tryRegister("ip_address", ConnectionString.type(ConnectionContext::addressString, false));
    public static final ConnectionCheckType USERNAME = REGISTRY.tryRegister("username", ConnectionString.type(ConnectionContext::username, true));
    public static final ConnectionCheckType UUID = REGISTRY.tryRegister("uuid", ConnectionString.type(ConnectionContext::uuidString, true));
    public static final ConnectionCheckType LOCALE = REGISTRY.tryRegister("locale", ConnectionString.type(ConnectionContext::locale, true));
    public static final ConnectionCheckType COOKIE = REGISTRY.tryRegister("cookie", CookieCheck.TYPE);
    public static final ConnectionCheckType COMPOSITE = REGISTRY.tryRegister("composite", Composite.TYPE);

    public static final ConnectionCheckType HOSTNAME_REGEX = REGISTRY.tryRegister("hostname_regex", RegexCheck.type(ConnectionContext::hostname, false));
    public static final ConnectionCheckType ADDRESS_REGEX = REGISTRY.tryRegister("ip_address_regex", RegexCheck.type(ConnectionContext::addressString, false));
    public static final ConnectionCheckType USERNAME_REGEX = REGISTRY.tryRegister("username_regex", RegexCheck.type(ConnectionContext::username, true));
    public static final ConnectionCheckType LOCALE_REGEX = REGISTRY.tryRegister("locale_regex", RegexCheck.type(ConnectionContext::locale, true));

}

package org.wallentines.mdproxy.requirement;

import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightlib.requirement.Check;
import org.wallentines.midnightlib.requirement.CheckType;

public interface ConnectionCheckType<T extends Check<ConnectionContext>> extends CheckType<ConnectionContext, T> {

    Registry<Identifier, CheckType<ConnectionContext, ?>> REGISTRY = Registry.create("mdp");

    static <T extends Check<ConnectionContext>> CheckType<ConnectionContext, T> register(String key, CheckType<ConnectionContext, T> type) {
        REGISTRY.tryRegister(key, type);
        return type;
    }

    CheckType<ConnectionContext, DateCheck> DATE = register("date", DateCheck.TYPE);
    CheckType<ConnectionContext, ConnectionString> HOSTNAME = register("hostname", new ConnectionString.Type(ConnectionContext::hostname, false));
    CheckType<ConnectionContext, ConnectionInt> PORT = register("port", new ConnectionInt.Type(ConnectionContext::port, false));
    CheckType<ConnectionContext, ConnectionString> ADDRESS = register("ip_address", new ConnectionString.Type(ConnectionContext::addressString, false));
    CheckType<ConnectionContext, ConnectionString> USERNAME = register("username", new ConnectionString.Type(ConnectionContext::username, true));
    CheckType<ConnectionContext, ConnectionString> UUID = register("uuid", new ConnectionString.Type(ConnectionContext::uuidString, true));
    CheckType<ConnectionContext, ConnectionString> LOCALE = register("locale", new ConnectionString.Type(ConnectionContext::locale, true));
    CheckType<ConnectionContext, CookieCheck> COOKIE = register("cookie", CookieCheck.TYPE);

    CheckType<ConnectionContext, RegexCheck> HOSTNAME_REGEX = register("hostname_regex", new RegexCheck.Type(ConnectionContext::hostname, false));
    CheckType<ConnectionContext, RegexCheck> ADDRESS_REGEX = register("ip_address_regex", new RegexCheck.Type(ConnectionContext::addressString, false));
    CheckType<ConnectionContext, RegexCheck> USERNAME_REGEX = register("username_regex", new RegexCheck.Type(ConnectionContext::username, true));
    CheckType<ConnectionContext, RegexCheck> LOCALE_REGEX = register("locale_regex", new RegexCheck.Type(ConnectionContext::locale, true));

    CheckType<ConnectionContext, Composite> COMPOSITE = register("composite", Composite.TYPE);
}

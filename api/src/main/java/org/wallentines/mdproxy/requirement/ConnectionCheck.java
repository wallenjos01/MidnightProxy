package org.wallentines.mdproxy.requirement;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.mdcfg.registry.Identifier;
import org.wallentines.midnightlib.requirement.Check;

import java.util.Collection;

public interface ConnectionCheck extends Check<ConnectionContext> {

    boolean requiresAuth();

    @NotNull
    Collection<Identifier> getRequiredCookies();

}

package org.wallentines.mdproxy;

import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.requirement.MultiRequirement;
import org.wallentines.midnightlib.requirement.Requirement;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class WrappedRequirement implements ConnectionRequirement {

    private final Requirement<ClientConnection> internal;

    public WrappedRequirement(Requirement<ClientConnection> internal) {
        this.internal = internal;
    }

    @Override
    public boolean requiresAuth() {

        if(internal instanceof MultiRequirement<ClientConnection> mr) {
            for(Requirement<ClientConnection> sr : mr.getRequirements()) {
                if(sr instanceof ConnectionRequirement cr && cr.requiresAuth()) return true;
            }
        }

        return internal instanceof ConnectionRequirement cr && cr.requiresAuth();
    }

    @Override
    public boolean requiresCookies() {

        if(internal instanceof MultiRequirement<ClientConnection> mr) {
            for(Requirement<ClientConnection> sr : mr.getRequirements()) {
                if(sr instanceof ConnectionRequirement cr && cr.requiresCookies()) return true;
            }
        }

        return internal instanceof ConnectionRequirement cr && cr.requiresCookies();
    }

    @Override
    public Collection<Identifier> getRequiredCookies() {

        if(internal instanceof MultiRequirement<ClientConnection> mr) {
            Set<Identifier> cookies = new HashSet<>();
            for(Requirement<ClientConnection> sr : mr.getRequirements()) {
                if(sr instanceof ConnectionRequirement cr) {
                    cookies.addAll(cr.getRequiredCookies());
                }
            }
            return cookies;
        } else if(internal instanceof ConnectionRequirement cr) {
            return cr.getRequiredCookies();
        }

        return null;
    }

    public boolean check(ClientConnection conn) {
        return internal.check(conn);
    }


    public static final Serializer<WrappedRequirement> SERIALIZER = Requirement.serializer(ConnectionRequirement.REGISTRY).map(o -> o.internal, WrappedRequirement::new);

}

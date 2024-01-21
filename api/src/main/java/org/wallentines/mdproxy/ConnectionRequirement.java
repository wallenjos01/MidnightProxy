package org.wallentines.mdproxy;

import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightlib.requirement.NumberRequirement;
import org.wallentines.midnightlib.requirement.RequirementType;
import org.wallentines.midnightlib.requirement.StringRequirement;

import java.util.Collection;

public interface ConnectionRequirement {

    boolean requiresAuth();

    boolean requiresCookies();

    Collection<Identifier> getRequiredCookies();

    Registry<RequirementType<ClientConnection>> REGISTRY = new Registry<>("mdp");


    RequirementType<ClientConnection> HOSTNAME = REGISTRY.register("hostname", StringRequirement.type(ClientConnection::hostname));
    RequirementType<ClientConnection> PORT = REGISTRY.register("port", NumberRequirement.type(ClientConnection::port));

}

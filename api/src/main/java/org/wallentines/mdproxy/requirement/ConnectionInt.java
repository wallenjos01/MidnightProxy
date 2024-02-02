package org.wallentines.mdproxy.requirement;

import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ClientConnection;
import org.wallentines.midnightlib.math.Range;
import org.wallentines.midnightlib.requirement.NumberCheck;

import java.util.function.Function;

public class ConnectionInt extends ConnectionCheck {

    private final Range<Integer> valid;
    private final Function<ClientConnection, Integer> getter;

    public ConnectionInt(Function<ClientConnection, Integer> getter, Range<Integer> valid, boolean requireAuth) {
        super(requireAuth, false, false, null);
        this.valid = valid;
        this.getter = getter;
    }

    @Override
    public boolean test(ClientConnection conn) {
        return valid.isWithin(getter.apply(conn));
    }

    public static Serializer<ConnectionInt> serializer(Function<ClientConnection, Integer> getter, boolean requireAuth) {
        return NumberCheck.serializer(Range.INTEGER, prt -> prt.valid, valid -> new ConnectionInt(getter, valid, requireAuth));
    }
}

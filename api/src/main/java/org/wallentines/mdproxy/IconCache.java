package org.wallentines.mdproxy;

import java.util.Optional;

public interface IconCache {

    Optional<String> getIconB64(String name);

}

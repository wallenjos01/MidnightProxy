package org.wallentines.mdproxy;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a cache for server list status icons.
 */
public interface IconCache {

    /**
     * Gets the base64-encoded PNG data for the icon with the given name, if present.
     * @param name The icon name.
     * @return The icon's data.
     */
    @Nullable
    String getIconB64(String name);

}

package org.wallentines.mdproxy;

import org.wallentines.pseudonym.text.ProtocolContext;

public record GameVersion(String name, int protocolVersion) {

    public static final int RELEASE_MAX_VERSION = 0x40000000;
    public static final GameVersion MAX = new GameVersion("Maximum", RELEASE_MAX_VERSION);

    public boolean hasFeature(ProtocolContext.Feature feature) {
        return hasFeature(protocolVersion, feature);
    }

    public static boolean hasFeature(int version, ProtocolContext.Feature feature) {
        if(version > RELEASE_MAX_VERSION) {
            return version - RELEASE_MAX_VERSION >= feature.snapshotVersion();
        }
        return version >= feature.version();
    }

}

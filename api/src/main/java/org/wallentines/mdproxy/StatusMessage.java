package org.wallentines.mdproxy;

import org.jetbrains.annotations.Nullable;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.text.ModernSerializer;
import org.wallentines.mdcfg.ConfigSection;

import java.util.Collection;

public record StatusMessage(GameVersion version, int playersOnline, int maxPlayers, Collection<PlayerInfo> playerSample,
                            Component message, @Nullable String favicon, boolean secureChat, boolean previewChat) {

    public ConfigSection serialize() {

        return new ConfigSection()
                .with("version", new ConfigSection()
                        .with("name", version.getId())
                        .with("protocol", version.getProtocolVersion())
                )
                .with("players", new ConfigSection()
                        .with("max", maxPlayers)
                        .with("online", playersOnline)
                        .with("sample", playerSample, PlayerInfo.SERIALIZER.filteredListOf())
                )
                .with("description", ModernSerializer.INSTANCE.serialize(GameVersion.context(version), message).getOrThrow())
                .with("favicon", favicon)
                .with("enforcesSecureChat", secureChat)
                .with("previewsChat", previewChat);
    }


}

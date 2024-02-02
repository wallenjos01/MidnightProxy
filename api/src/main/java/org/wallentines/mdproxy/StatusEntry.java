package org.wallentines.mdproxy;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.text.ModernSerializer;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.requirement.ConnectionRequirement;

import java.util.Collection;
import java.util.List;

public record StatusEntry(int priority, Integer playersOverride, Integer maxPlayersOverride,
                          Collection<PlayerInfo> playerSample, Component message, String icon, Boolean secureChat,
                          Boolean previewChat, String passthrough, ConnectionRequirement requirement) implements Comparable<StatusEntry> {

    public boolean shouldPassthrough() {
        return passthrough != null;
    }

    @Override
    public int compareTo(@NotNull StatusEntry o) {
        return o.priority - priority;
    }

    public StatusMessage create(GameVersion version, int onlinePlayers, int maxPlayers) {

        if(playersOverride != null) onlinePlayers = playersOverride;
        if(maxPlayersOverride != null) maxPlayers = maxPlayersOverride;

        return new StatusMessage(version, onlinePlayers, maxPlayers,
                playerSample == null ? List.of() : playerSample,
                message == null ? Component.empty() : message,
                null,
                secureChat != null && secureChat,
                previewChat != null && previewChat);
    }

    public ConfigSection resolve(ConfigSection other, IconCache cache) {

        GameVersion ver = GameVersion.MAX;
        if(other.hasSection("version")) {
            ConfigSection version = other.getSection("version");
            ver = new GameVersion(version.getString("name"), version.getInt("protocol"));
        }

        ConfigSection out = other.copy();
        if(playersOverride != null) {
            out.getOrCreateSection("players").set("online", playersOverride);
        }
        if(maxPlayersOverride != null) {
            out.getOrCreateSection("players").set("max", maxPlayersOverride);
        }
        if(playerSample != null) {
            out.getOrCreateSection("players").set("sample", playerSample, PlayerInfo.SERIALIZER.listOf());
        }
        if(message != null) {
            out.set("description", message, ModernSerializer.INSTANCE.forContext(ver));
        }
        if(icon != null) {
            String iconB64 = cache.getIconB64(icon);
            if(iconB64 != null) {
                out.set("favicon", iconB64);
            }
        }
        if(secureChat != null) {
            out.set("enforcesSecureChat", secureChat);
        }
        if(previewChat != null) {
            out.set("previewsChat", previewChat);
        }

        return out;
    }

    public boolean canUse(ConnectionContext ctx) {

        return requirement == null || requirement.test(ctx) == TestResult.PASS;
    }

    public static final Serializer<StatusEntry> SERIALIZER = ObjectSerializer.create(
            Serializer.INT.entry("priority", StatusEntry::priority).orElse(0),
            Serializer.INT.entry("players_override", StatusEntry::playersOverride).optional(),
            Serializer.INT.entry("max_players_override", StatusEntry::maxPlayersOverride).optional(),
            PlayerInfo.SERIALIZER.listOf().entry("player_sample", StatusEntry::playerSample).optional(),
            ModernSerializer.INSTANCE.forContext(GameVersion.MAX).entry("message", StatusEntry::message).optional(),
            Serializer.STRING.entry("icon", StatusEntry::icon).optional(),
            Serializer.BOOLEAN.entry("secure_chat", StatusEntry::secureChat).optional(),
            Serializer.BOOLEAN.entry("preview_chat", StatusEntry::previewChat).optional(),
            Serializer.STRING.entry("passthrough", StatusEntry::passthrough).optional(),
            ConnectionRequirement.SERIALIZER.entry("requirement", StatusEntry::requirement).optional(),
            StatusEntry::new
    );


}

package com.github.neveshardd.titles;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.bukkit.entity.Player;

final class ClientVersions {

    private final boolean viaVersion;

    ClientVersions(boolean viaVersion) {
        this.viaVersion = viaVersion;
    }

    boolean legacy(Player player) {
        if (!viaVersion) {
            return false;
        }
        ProtocolVersion version = Via.getAPI().getPlayerProtocolVersion(player.getUniqueId());
        return version.isKnown() && version.olderThan(ProtocolVersion.v1_9);
    }
}

package com.github.neveshardd.titles;

import org.bukkit.entity.Player;

public record Title(String id, String text, String permission) {

    public boolean isUnlocked(Player player) {
        return permission.isEmpty() || player.hasPermission(permission);
    }
}

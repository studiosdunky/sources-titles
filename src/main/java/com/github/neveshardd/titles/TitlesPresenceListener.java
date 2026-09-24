package com.github.neveshardd.titles;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class TitlesPresenceListener implements Listener {

    private final Plugin plugin;
    private final TitleService titles;
    private final TitleDisplay display;

    public TitlesPresenceListener(Plugin plugin, TitleService titles, TitleDisplay display) {
        this.plugin = plugin;
        this.titles = titles;
        this.display = display;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        titles.load(playerId).thenRun(() -> {
            if (!plugin.isEnabled()) {
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    titles.unload(playerId);
                    return;
                }
                display.refresh(player);
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        display.despawn(event.getPlayer().getUniqueId());
        titles.unload(event.getPlayer().getUniqueId());
    }
}

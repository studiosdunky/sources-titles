package com.github.neveshardd.titles;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TitleDisplay implements Listener {

    private final Plugin plugin;
    private final TitleService titles;
    private final TitleRegistry registry;
    private final ClientVersions versions;
    private final Map<UUID, TitleStack> stacks = new ConcurrentHashMap<>();

    public TitleDisplay(Plugin plugin, TitleService titles, TitleRegistry registry, ClientVersions versions) {
        this.plugin = plugin;
        this.titles = titles;
        this.registry = registry;
        this.versions = versions;
    }

    public void refresh(Player player) {
        Optional<Title> title = titles.equippedTitle(player.getUniqueId());
        if (title.isEmpty() || title.get().text().isEmpty()) {
            despawn(player.getUniqueId());
            return;
        }

        Component name = color(title.get().text());
        TitleStack stack = stacks.get(player.getUniqueId());
        if (stack != null) {
            stack.rename(name, visibleViewers(stack));
            return;
        }

        stack = new TitleStack(player, name, registry.legacySlimeSize(), registry.modernSlimeSize(), registry.modernSlimeScale());
        stacks.put(player.getUniqueId(), stack);
        showToTrackers(stack);
    }

    public void despawn(UUID uuid) {
        TitleStack stack = stacks.remove(uuid);
        if (stack != null) {
            hideFromTrackers(stack);
        }
    }

    public void despawnAll() {
        for (TitleStack stack : stacks.values()) {
            hideFromTrackers(stack);
        }
        stacks.clear();
    }

    public void rebuildAll(Collection<? extends Player> players) {
        despawnAll();
        for (Player player : players) {
            refresh(player);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTrack(PlayerTrackEntityEvent event) {
        if (!(event.getEntity() instanceof Player owner)) {
            return;
        }
        Player viewer = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            TitleStack stack = stacks.get(owner.getUniqueId());
            if (stack != null && visible(stack) && owner.getTrackedBy().contains(viewer)) {
                stack.show(viewer, versions.legacy(viewer));
            }
        });
    }

    @EventHandler
    public void onUntrack(PlayerUntrackEntityEvent event) {
        if (event.getEntity() instanceof Player owner) {
            TitleStack stack = stacks.get(owner.getUniqueId());
            if (stack != null) {
                stack.hide(event.getPlayer());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player owner = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            TitleStack stack = stacks.get(owner.getUniqueId());
            if (stack == null) {
                return;
            }
            hideFromTrackers(stack);
            showToTrackers(stack);
        });
    }

    @EventHandler
    public void onUseUnknownEntity(PlayerUseUnknownEntityEvent event) {
        if (!event.isAttack()) {
            return;
        }
        int entityId = event.getEntityId();
        for (TitleStack stack : stacks.values()) {
            if (stack.owns(entityId)) {
                Player attacker = event.getPlayer();
                Player owner = stack.owner();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (attacker.isOnline() && owner.isOnline() && attacker.getWorld().equals(owner.getWorld())) {
                        attacker.attack(owner);
                    }
                });
                return;
            }
        }
    }

    private void showToTrackers(TitleStack stack) {
        if (!visible(stack)) {
            return;
        }
        for (Player viewer : stack.owner().getTrackedBy()) {
            stack.show(viewer, versions.legacy(viewer));
        }
    }

    private void hideFromTrackers(TitleStack stack) {
        for (Player viewer : stack.owner().getTrackedBy()) {
            stack.hide(viewer);
        }
    }

    private Iterable<? extends Player> visibleViewers(TitleStack stack) {
        return visible(stack) ? stack.owner().getTrackedBy() : List.of();
    }

    private static boolean visible(TitleStack stack) {
        return stack.owner().getGameMode() != GameMode.SPECTATOR;
    }

    private static Component color(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}

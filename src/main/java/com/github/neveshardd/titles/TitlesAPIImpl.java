package com.github.neveshardd.titles;

import com.github.neveshardd.api.titles.TitleInfo;
import com.github.neveshardd.api.titles.TitlesAPI;
import org.bukkit.entity.Player;

import java.util.List;

final class TitlesAPIImpl implements TitlesAPI {

    private final TitleRegistry registry;
    private final TitleService titles;
    private final TitleDisplay display;

    TitlesAPIImpl(TitleRegistry registry, TitleService titles, TitleDisplay display) {
        this.registry = registry;
        this.titles = titles;
        this.display = display;
    }

    @Override
    public List<TitleInfo> unlockedTitles(Player player) {
        String equippedId = titles.equippedTitleId(player.getUniqueId());
        return registry.all().stream()
                .filter(title -> title.isUnlocked(player))
                .map(title -> new TitleInfo(title.id(), title.text(), title.id().equalsIgnoreCase(equippedId)))
                .toList();
    }

    @Override
    public void equip(Player player, String titleId) {
        titles.equip(player.getUniqueId(), titleId);
        display.refresh(player);
    }
}

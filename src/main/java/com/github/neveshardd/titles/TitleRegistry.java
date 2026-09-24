package com.github.neveshardd.titles;

import com.github.neveshardd.api.config.ConfigAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TitleRegistry {

    private final ConfigAPI config;
    private volatile List<Title> titles;
    private volatile String defaultTitleId;
    private volatile int legacySlimeSize;
    private volatile int modernSlimeSize;
    private volatile double modernSlimeScale;

    public TitleRegistry(ConfigAPI config) {
        this.config = config;
        load();
    }

    public void reload() {
        config.reload();
        load();
    }

    private void load() {
        List<Title> loaded = new ArrayList<>();
        ConfigAPI section = config.getSection("titles");
        for (String id : section.getKeys()) {
            ConfigAPI entry = section.getSection(id);
            loaded.add(new Title(id, entry.getString("text", ""), entry.getString("permission", "")));
        }
        this.titles = List.copyOf(loaded);
        this.defaultTitleId = config.getString("default-title", "");
        this.legacySlimeSize = Math.max(1, config.getInt("slime-size.legacy", 2));
        this.modernSlimeSize = Math.max(1, config.getInt("slime-size.modern", 1));
        this.modernSlimeScale = Math.clamp(parseDecimal(config.getString("slime-scale.modern", "1.0"), 1.0), 0.0625, 16.0);
    }

    public List<Title> all() {
        return titles;
    }

    public Optional<Title> find(String id) {
        return titles.stream().filter(title -> title.id().equalsIgnoreCase(id)).findFirst();
    }

    public String defaultTitleId() {
        return defaultTitleId;
    }

    public int legacySlimeSize() {
        return legacySlimeSize;
    }

    public int modernSlimeSize() {
        return modernSlimeSize;
    }

    public double modernSlimeScale() {
        return modernSlimeScale;
    }

    private static double parseDecimal(String raw, double fallback) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

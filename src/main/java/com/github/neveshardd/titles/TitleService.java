package com.github.neveshardd.titles;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TitleService {

    private final TitlesStore store;
    private final TitleRegistry registry;
    private final Logger logger;
    private final ExecutorService database = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "hTitles-banco");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();

    public TitleService(TitlesStore store, TitleRegistry registry, Logger logger) {
        this.store = store;
        this.registry = registry;
        this.logger = logger;
    }

    public String equippedTitleId(UUID uuid) {
        return cache.getOrDefault(uuid, registry.defaultTitleId());
    }

    public Optional<Title> equippedTitle(UUID uuid) {
        String titleId = equippedTitleId(uuid);
        return titleId.isEmpty() ? Optional.empty() : registry.find(titleId);
    }

    public CompletableFuture<Void> load(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                cache.put(uuid, store.loadTitle(uuid).orElseGet(registry::defaultTitleId));
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Não foi possível carregar o titulo de " + uuid, e);
            }
        }, database);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public void equip(UUID uuid, String titleId) {
        cache.put(uuid, titleId);
        database.execute(() -> {
            try {
                store.saveTitle(uuid, titleId);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Não foi possível salvar o titulo de " + uuid, e);
            }
        });
    }

    public void close() {
        database.shutdown();
        try {
            database.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

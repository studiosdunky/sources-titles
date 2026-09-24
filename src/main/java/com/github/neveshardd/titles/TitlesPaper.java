package com.github.neveshardd.titles;

import com.github.neveshardd.api.command.CommandDoc;
import com.github.neveshardd.api.command.CommandRegistryAPI;
import com.github.neveshardd.api.config.ConfigAPI;
import com.github.neveshardd.api.config.ConfigManagerAPI;
import com.github.neveshardd.api.core.CoreLifecycleAPI;
import com.github.neveshardd.api.database.DatabaseAPI;
import com.github.neveshardd.api.message.ChatMessengerAPI;
import com.github.neveshardd.api.titles.TitlesAPI;
import com.github.neveshardd.core.Core;
import com.github.neveshardd.core.command.CommandCatalog;
import com.github.neveshardd.core.command.CommandHelp;
import com.github.neveshardd.core.config.ConfigManager;
import com.github.neveshardd.core.permission.PermissionsFile;
import com.github.neveshardd.paper.api.PaperChatMessenger;
import com.github.neveshardd.paper.api.PaperCommandRegistry;
import com.github.neveshardd.paper.api.PaperLog;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class TitlesPaper extends JavaPlugin {

    private CoreLifecycleAPI core;
    private TitleService titleService;
    private TitleDisplay display;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<DatabaseAPI> databaseProvider = getServer().getServicesManager().getRegistration(DatabaseAPI.class);
        DatabaseAPI database = databaseProvider != null ? databaseProvider.getProvider() : null;
        if (database == null) {
            getLogger().severe("Conexão com o banco de dados não estabelecida.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        core = new Core("hTitles", new PaperLog(getServer().getLogger()));
        core.enable();

        ConfigManagerAPI configs = new ConfigManager(getDataFolder().toPath(), getClass().getClassLoader());
        ConfigAPI titlesConfig = configs.load("titles.yml");
        TitleRegistry registry = new TitleRegistry(titlesConfig);
        titleService = new TitleService(new TitlesStore(database), registry, getLogger());
        ClientVersions versions = new ClientVersions(getServer().getPluginManager().isPluginEnabled("ViaVersion"));
        display = new TitleDisplay(this, titleService, registry, versions);

        ChatMessengerAPI chat = new PaperChatMessenger();
        CommandRegistryAPI commands = new PaperCommandRegistry(this);
        CommandCatalog catalog = new CommandCatalog(3);
        CommandHelp help = new CommandHelp(chat, catalog);

        TitlesCommand titlesCommand = new TitlesCommand(registry, display, this, chat, help);
        catalog.add(new CommandDoc("/titulos reload", "Recarrega o titles.yml"));
        commands.register("titulos", "", titlesCommand);
        commands.register("titles", "", titlesCommand);

        getServer().getPluginManager().registerEvents(display, this);
        getServer().getPluginManager().registerEvents(new TitlesPresenceListener(this, titleService, display), this);

        getServer().getServicesManager().register(TitlesAPI.class, new TitlesAPIImpl(registry, titleService, display), this, ServicePriority.Normal);

        for (Player player : getServer().getOnlinePlayers()) {
            titleService.load(player.getUniqueId()).thenRun(() -> {
                if (!isEnabled()) {
                    return;
                }
                getServer().getScheduler().runTask(this, () -> {
                    if (player.isOnline()) {
                        display.refresh(player);
                    }
                });
            });
        }

        PermissionsFile.publish(getDataFolder().toPath(), getClass().getClassLoader());
        registerKnownPermissions();
    }

    private void registerKnownPermissions() {
        for (String node : PermissionsFile.readNodes(getClass().getClassLoader())) {
            if (getServer().getPluginManager().getPermission(node) == null) {
                getServer().getPluginManager().addPermission(new Permission(node, PermissionDefault.OP));
            }
        }
    }

    @Override
    public void onDisable() {
        if (display != null) {
            display.despawnAll();
        }
        if (titleService != null) {
            titleService.close();
        }
        if (core != null) {
            core.disable();
        }
    }
}

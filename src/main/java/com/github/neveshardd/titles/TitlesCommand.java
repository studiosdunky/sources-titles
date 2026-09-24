package com.github.neveshardd.titles;

import com.github.neveshardd.api.command.CommandAPI;
import com.github.neveshardd.api.command.CommandDoc;
import com.github.neveshardd.api.command.CommandSenderAPI;
import com.github.neveshardd.api.message.ChatMessengerAPI;
import com.github.neveshardd.core.command.CommandHelp;
import org.bukkit.plugin.Plugin;

import java.util.List;

final class TitlesCommand implements CommandAPI {

    private static final String RELOAD_PERMISSION = "titles.reload";

    private final TitleRegistry registry;
    private final TitleDisplay display;
    private final Plugin plugin;
    private final ChatMessengerAPI chat;
    private final CommandHelp help;

    TitlesCommand(TitleRegistry registry, TitleDisplay display, Plugin plugin, ChatMessengerAPI chat, CommandHelp help) {
        this.registry = registry;
        this.display = display;
        this.plugin = plugin;
        this.chat = chat;
        this.help = help;
    }

    @Override
    public void execute(CommandSenderAPI sender, String[] args) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("recarregar"))) {
            if (help.requirePermission(sender, RELOAD_PERMISSION)) {
                return;
            }
            registry.reload();
            plugin.getServer().getScheduler().runTask(plugin, () -> display.rebuildAll(plugin.getServer().getOnlinePlayers()));
            chat.send(sender.uuid(), " &aArquivo titles.yml recarregado.");
            return;
        }

        help.send(sender.uuid(), List.of(new CommandDoc("/titulos reload", "Recarrega o titles.yml")));
    }
}

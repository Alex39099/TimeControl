package com.github.alexqp.timecontrol.command;

import com.github.alexqp.commons.command.AlexSubCommand;
import com.github.alexqp.timecontrol.main.TimeControl;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ReloadSubCmd extends AlexSubCommand {

    private final TimeControl plugin;
    private final BaseComponent success;

    ReloadSubCmd(@NotNull AlexSubCommand parent, @NotNull TimeControl plugin) {
        super("reload", "reloads the plugin.", parent);
        this.plugin = plugin;
        this.success = this.getPrefixMessage(new ComponentBuilder("Plugin reloaded!").color(ChatColor.DARK_GREEN).create());
        this.makeFinal();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label,
                           @NotNull List<AlexSubCommand> previousCmds, @NotNull List<String> previousExtraArguments,
                           @NotNull String[] args, int startIndex) {
        this.plugin.onReload();
        sendMessage(sender, success);
        return true;
    }
}

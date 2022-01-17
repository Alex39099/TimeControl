package com.github.alexqp.timecontrol.command;

import com.github.alexqp.commons.command.AlexSubCommand;
import com.github.alexqp.timecontrol.data.TimeWorld;
import com.github.alexqp.timecontrol.data.WorldContainer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DebugSubCmd extends AlexSubCommand {

    private final WorldContainer worldContainer;

    private final BaseComponent debugHeader;

    DebugSubCmd(@NotNull AlexSubCommand parent, @NotNull JavaPlugin plugin, @NotNull WorldContainer worldContainer) {
        super("debug", "shows some debug messages.", parent);
        this.worldContainer = worldContainer;
        this.debugHeader = this.getPrefixMessage(new ComponentBuilder("Debug messages: (" + plugin.getName() + " v" + plugin.getDescription().getVersion() + ")" ).color(ChatColor.DARK_PURPLE).create());
        this.makeFinal();
    }

    private String getEnabledWorlds() {
        List<String> worldNames = new ArrayList<>();
        for (World world : Bukkit.getServer().getWorlds()) {
            if (worldContainer.isEnabled(world)) {
                worldNames.add(world.getName());
            }
        }
        return worldNames.toString();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label,
                           @NotNull List<AlexSubCommand> previousCmds, @NotNull List<String> previousExtraArguments,
                           @NotNull String[] args, int startIndex) {
        String headingsPrefix = "" + ChatColor.GOLD;

        sendMessage(sender, debugHeader);
        sendMessage(sender, this.getPrefixMessage(new TextComponent(headingsPrefix + "Current config and enabled worlds:")));
        sendMessage(sender, this.getPrefixMessage(new TextComponent("ConfigWorlds = " + worldContainer.getConfigWorldNames())));
        sendMessage(sender, this.getPrefixMessage(new TextComponent("Enabled Worlds = " + this.getEnabledWorlds())));
        sendMessage(sender, this.getPrefixMessage(new TextComponent()));
        sendMessage(sender, this.getPrefixMessage(new TextComponent(headingsPrefix + "Current World-Data:")));
        sendMessage(sender, this.getPrefixMessage(new ComponentBuilder("Prefixes = ").color(ChatColor.YELLOW).append(TimeWorld.getPrefixes()).reset().create()));
        sendMessage(sender, this.getPrefixMessage(new TextComponent()));
        for (String worldName : worldContainer.getConfigWorldNames()) {
            sendMessage(sender, this.getPrefixMessage(new ComponentBuilder(worldName + ": ").color(ChatColor.YELLOW).append(worldContainer.getTimeWorldForWorld(worldName).toString()).reset().create()));
        }
        return true;
    }
}

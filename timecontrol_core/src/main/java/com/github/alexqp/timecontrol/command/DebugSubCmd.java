/*
 * Copyright (C) 2018-2024 Alexander Schmid
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

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
import java.util.Objects;

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
        sendMessage(sender, this.getPrefixMessage(new TextComponent("ConfigWorlds = " + worldContainer.getLoadedWorlds())));
        sendMessage(sender, this.getPrefixMessage(new TextComponent("Enabled Worlds = " + this.getEnabledWorlds())));
        sendMessage(sender, this.getPrefixMessage(new TextComponent()));
        sendMessage(sender, this.getPrefixMessage(new TextComponent(headingsPrefix + "Current World-Data:")));
        sendMessage(sender, this.getPrefixMessage(new ComponentBuilder("Prefixes = ").color(ChatColor.YELLOW).append(TimeWorld.getPrefixes()).reset().create()));
        sendMessage(sender, this.getPrefixMessage(new TextComponent()));
        for (String worldName : worldContainer.getLoadedWorlds()) {
            sendMessage(sender, this.getPrefixMessage(new ComponentBuilder(worldName + ": ").color(ChatColor.YELLOW).append(Objects.requireNonNull(worldContainer.getTimeWorldForWorld(worldName)).toString()).reset().create()));
        }
        return true;
    }
}

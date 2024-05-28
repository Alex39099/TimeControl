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
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;
import com.github.alexqp.timecontrol.data.WorldContainer;
import com.github.alexqp.timecontrol.main.InternalsProvider;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UninstallSubCmd extends AlexSubCommand {

    private final JavaPlugin plugin;
    private final InternalsProvider internals;
    private final WorldContainer worldContainer;

    UninstallSubCmd(@NotNull AlexSubCommand parent, @NotNull JavaPlugin plugin, @NotNull InternalsProvider internals, @NotNull WorldContainer worldContainer) {
        super("uninstall", "Re-enables all gameRules and disables the plugin.", parent);
        this.setIsPlayerCmd(false);
        this.plugin = plugin;
        this.worldContainer = worldContainer;
        this.internals = internals;
        this.makeFinal();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label,
                           @NotNull List<AlexSubCommand> previousCmds, @NotNull List<String> previousExtraArguments,
                           @NotNull String[] args, int startIndex) {
        for (String worldName : worldContainer.getLoadedWorlds()) {
            World world = Bukkit.getServer().getWorld(worldName);
            if (world != null) {
                if (internals.handleGameRules(world, true)) {
                    plugin.getLogger().info("(re-)enabled GameRules for world " + worldName);
                }
            } else {
                ConsoleMessage.debug(this.getClass(), plugin, "could not find a valid world for configWorld " + worldName);
            }
        }
        plugin.getLogger().info("If you had problems with the plugin feel free to contact me (alex_qp) on spigot.");
        plugin.getLogger().info("If you were unhappy with the plugin because of bad performance or sth like that please consider leaving a review/suggestion to help me improve.");
        plugin.getLogger().info("Plugin will now disable, please delete the jar file before restarting/reloading your server");
        plugin.onDisable();
        return true;
    }
}

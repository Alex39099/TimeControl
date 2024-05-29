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

package com.github.alexqp.timecontrol.main;

import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class InternalsProvider {

    public InternalsProvider() {}

    public boolean handleGameRules(@NotNull World world) {
        return this.handleGameRules(world, false);
    }

    public boolean handleGameRules(@NotNull World world, boolean enable) {
        world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, enable ? 100 : 101);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, enable);
        return true;
    }

    public boolean needAllLeftBedTrigger() {
        return false;
    }

    public void disableSleepActionBar(@NotNull JavaPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            ProtocolLib.disableSleepActionBar(plugin);
        } else {
            ConsoleMessage.send(ConsoleErrorType.WARN, plugin, "ProtocolLib is not installed on your server. Vanilla-Sleep-Messages will not be disabled");
        }
    }
}

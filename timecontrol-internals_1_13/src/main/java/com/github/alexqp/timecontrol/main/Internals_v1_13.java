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

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Internals_v1_13 extends InternalsProvider {

    public boolean handleGameRules(@NotNull World world, boolean enable) {
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, enable);
        return true;
    }

    public boolean needAllLeftBedTrigger() {
        return true;
    }

    @Override
    public void disableSleepActionBar(@NotNull JavaPlugin plugin) {
    }
}

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

package com.github.alexqp.timecontrol.sleep_mechanic;

import com.github.alexqp.commons.messages.ConsoleMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AllLeftBedTrigger implements Listener {

    private final Map<String, Set<UUID>> bedLeaveMap = new HashMap<>();

    public AllLeftBedTrigger(@NotNull JavaPlugin plugin, @NotNull SleepManager sleepManager, int period) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String worldName : bedLeaveMap.keySet()) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null && didAllNotSleepingIgnoredPlayersLeaveBed(world)) {
                        ConsoleMessage.debug(AllLeftBedTrigger.class, plugin, "All left the bed at once.");
                        sleepManager.setHardSleeping(world, true);
                    }
                }
                bedLeaveMap.clear();
            }
        }.runTaskTimer(plugin, period, period);
    }

    public boolean didAllNotSleepingIgnoredPlayersLeaveBed(@NotNull World world) {
        Set<UUID> bedLeavePlayers = bedLeaveMap.get(world.getName());
        if (bedLeavePlayers != null && bedLeavePlayers.size() > 0) {
            for (Player p : world.getPlayers()) {
                if (p.isSleepingIgnored())
                    bedLeavePlayers.add(p.getUniqueId());
            }
            bedLeaveMap.remove(world.getName());
            return world.getPlayers().size() <= bedLeavePlayers.size();
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedLeave(PlayerBedLeaveEvent e) {
        Set<UUID> bedLeavePlayers = bedLeaveMap.computeIfAbsent(e.getPlayer().getWorld().getName(), k -> new HashSet<>());
        bedLeavePlayers.add(e.getPlayer().getUniqueId());
    }
}

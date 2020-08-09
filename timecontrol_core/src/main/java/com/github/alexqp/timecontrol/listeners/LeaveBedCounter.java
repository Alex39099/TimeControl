package com.github.alexqp.timecontrol.listeners;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedLeaveEvent;

import java.util.*;

public class LeaveBedCounter implements Listener {

    private Map<String, Set<UUID>> bedLeaveMap = new HashMap<>();

    public LeaveBedCounter() {
    }

    public boolean didAllNotSleepingIgnoredPlayersLeaveBed(World world) {
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

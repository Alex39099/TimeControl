package com.github.alexqp.timecontrol.listeners;


import com.github.alexqp.commons.messages.ConsoleMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.github.alexqp.timecontrol.data.WorldContainer;

import java.util.*;

public class BedListener implements Listener {

    private final long sleepDelay = 101;

    private JavaPlugin plugin;
    private WorldContainer worldContainer;

    private Map<String, Map<UUID, BukkitRunnable>> leaveBedPlayers = new HashMap<>();

    public BedListener(JavaPlugin plugin, WorldContainer container) {
        this.plugin = plugin;
        this.worldContainer = container;
    }

    public void setWorldContainer(WorldContainer container) {
        this.worldContainer = container;
    }

    private boolean hasAlreadyLeftBed(Player p) {
        Map<UUID, BukkitRunnable> worldLeaveBedPlayers = leaveBedPlayers.computeIfAbsent(p.getWorld().getName(), k -> new HashMap<>());
        BukkitRunnable runnable = worldLeaveBedPlayers.get(p.getUniqueId());
        if (runnable != null) {
            runnable.cancel();
            worldLeaveBedPlayers.remove(p.getUniqueId());
            return true;
        }
        return false;
    }

    private void addLeaveBedPlayer(Player p) {

        // cancel existing task...
        this.hasAlreadyLeftBed(p);

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                Map<UUID, BukkitRunnable> worldLeaveBedPlayers = leaveBedPlayers.computeIfAbsent(p.getWorld().getName(), k -> new HashMap<>());
                worldLeaveBedPlayers.remove(p.getUniqueId());
            }
        };

        Map<UUID, BukkitRunnable> worldLeaveBedPlayers = leaveBedPlayers.computeIfAbsent(p.getWorld().getName(), k -> new HashMap<>());

        worldLeaveBedPlayers.put(p.getUniqueId(), runnable);
        runnable.runTaskLater(plugin, sleepDelay);

        worldContainer.getTimeWorldForWorld(p.getWorld()).removeSleepingPlayer(p);
        ConsoleMessage.debug(this.getClass(), plugin, "removed sleeping player " + p.getName() + " for world " + p.getWorld().getName());
    }

    @EventHandler
    private void onBedLeave(PlayerBedLeaveEvent e) {
        ConsoleMessage.debug(this.getClass(), plugin, "BedLeave fired!");
        if (worldContainer.isConfigEnabled(e.getPlayer().getWorld())) {
            this.addLeaveBedPlayer(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBedEnter(PlayerBedEnterEvent e) {
        ConsoleMessage.debug(this.getClass(), plugin, "BedEnter fired!");
        if (worldContainer.isConfigEnabled(e.getPlayer().getWorld())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Player p = e.getPlayer();

                    if (!hasAlreadyLeftBed(p)) {
                        worldContainer.getTimeWorldForWorld(p.getWorld()).addSleepingPlayer(p);
                        ConsoleMessage.debug(BedListener.class, plugin, "added sleeping player " + p.getName() + " for world " + p.getWorld().getName());
                    } else {
                        ConsoleMessage.debug(BedListener.class, plugin, "did not add sleeping player " + p.getName() + " bc he left the bed beforehand.");
                    }
                }

            }.runTaskLater(plugin, sleepDelay);
        }
    }
}

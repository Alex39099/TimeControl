package com.github.alexqp.timecontrol.sleep_mechanic;


import com.github.alexqp.commons.messages.ConsoleMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BedListener implements Listener {

    private final int sleepDelay = 101;

    private JavaPlugin plugin;
    private SleepObserver sleepObserver;

    private Map<String, Map<UUID, BukkitRunnable>> leaveBedPlayers = new HashMap<>();

    public BedListener(@NotNull JavaPlugin plugin, @NotNull SleepObserver sleepObserver) {
        this.plugin = plugin;
        this.sleepObserver = sleepObserver;
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

        sleepObserver.removeSleepingPlayer(p);
        ConsoleMessage.debug(this.getClass(), plugin, "removed sleeping player " + p.getName() + " for world " + p.getWorld().getName());
    }

    @EventHandler
    private void onBedLeave(PlayerBedLeaveEvent e) {
        ConsoleMessage.debug(this.getClass(), plugin, "BedLeave fired!");
        if (sleepObserver.isSleepObserved(e.getPlayer().getWorld())) {
            this.addLeaveBedPlayer(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBedEnter(PlayerBedEnterEvent e) {
        ConsoleMessage.debug(this.getClass(), plugin, "BedEnter fired!");
        Player p = e.getPlayer();
        if (sleepObserver.isSleepObserved(p.getWorld())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!hasAlreadyLeftBed(p)) {
                        ConsoleMessage.debug(BedListener.class, plugin, "added sleeping player " + p.getName() + " for world " + p.getWorld().getName());
                        sleepObserver.addSleepingPlayer(p);
                    } else {
                        ConsoleMessage.debug(BedListener.class, plugin, "did not add sleeping player " + p.getName() + " bc he left the bed beforehand.");
                    }
                }

            }.runTaskLater(plugin, sleepDelay);
        }
    }
}

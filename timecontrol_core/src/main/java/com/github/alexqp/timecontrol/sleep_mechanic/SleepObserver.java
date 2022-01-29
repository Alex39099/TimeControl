package com.github.alexqp.timecontrol.sleep_mechanic;

import com.earth2me.essentials.Essentials;
import com.github.alexqp.commons.config.ConfigChecker;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SleepObserver implements Listener {

    @NotNull
    public static SleepObserver build(@NotNull JavaPlugin plugin, @NotNull SleepManager sleepManager) {
        ConfigChecker configChecker = new ConfigChecker(plugin);

        ConfigurationSection section = configChecker.checkConfigSection(plugin.getConfig(), "gamemode_sleeping", ConsoleErrorType.ERROR);
        HashSet<GameMode> sleepingGameModes = new HashSet<>();
        if (section != null) {
            for (GameMode gameMode : GameMode.values()) {
                if (configChecker.checkBoolean(section, gameMode.name().toLowerCase(), ConsoleErrorType.WARN, false)) {
                    sleepingGameModes.add(gameMode);
                }
            }
        }

        section = configChecker.checkConfigSection(plugin.getConfig(), "essentials", ConsoleErrorType.ERROR);
        boolean useEssentialsAFK = false;
        if (section != null) {
            useEssentialsAFK = configChecker.checkBoolean(section, "afk_is_sleeping", ConsoleErrorType.WARN, false);
        }

        return new SleepObserver(plugin, sleepManager, sleepingGameModes, useEssentialsAFK);
    }

    private final JavaPlugin plugin;
    private final SleepManager sleepManager;

    private final Collection<GameMode> sleepingGameModes;
    private Essentials essentials = null;

    private final HashMap<String, HashSet<UUID>> allSleepingPlayers = new HashMap<>();

    public SleepObserver(@NotNull JavaPlugin plugin, @NotNull SleepManager sleepManager, @NotNull Collection<GameMode> sleepingGameModes, boolean useEssentialsAFK) {
        this.plugin = plugin;
        this.sleepManager = sleepManager;

        this.sleepingGameModes = sleepingGameModes;
        if (useEssentialsAFK) {
            essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
            if (essentials == null) {
                ConsoleMessage.send(ConsoleErrorType.ERROR, plugin, "Essentials could not be found despite essentials.afk_is_sleeping was active.");
            }
        }

        Bukkit.getPluginManager().registerEvents(new BedListener(plugin, this), plugin);
        ConsoleMessage.debug(this.getClass(), plugin, "Registered BedListeners.");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        ConsoleMessage.debug(this.getClass(), plugin, "Registered WorldChangeListeners.");
    }

    public void addSleepingPlayer(@NotNull Player p) {
        World world = p.getWorld();
        if (this.isSleepObserved(world)) {
            allSleepingPlayers.computeIfAbsent(world.getName(), k -> new HashSet<>()).add(p.getUniqueId());
            ConsoleMessage.debug(this.getClass(), plugin, "Added sleeping player " + ConsoleMessage.getPlayerString(p));
            sleepManager.addSleepingPlayer(p);
        }
    }

    public void removeSleepingPlayer(@NotNull Player p) {
        World world = p.getWorld();
        HashSet<UUID> sleepingPlayers = allSleepingPlayers.get(world.getName());
        if (this.isSleepObserved(world) && sleepingPlayers != null) {
            if (sleepingPlayers.remove(p.getUniqueId())) {
                ConsoleMessage.debug(this.getClass(), plugin, "Removed sleeping player " + ConsoleMessage.getPlayerString(p));
                sleepManager.removeSleepingPlayer(p);
            }
        }
    }

    public boolean isSleepObserved(@Nullable World world) {
        return sleepManager.isSleepObserved(world);
    }

    private boolean isSleepingIgnored(@NotNull Player p) {
        return p.isSleepingIgnored()
                || p.hasPermission("timecontrol.sleepingignored")
                || sleepingGameModes.contains(p.getGameMode())
                || (essentials != null && essentials.getUser(p).isAfk());
    }

    public int getSleeping(@NotNull World world) {
        return Math.toIntExact(allSleepingPlayers.getOrDefault(world.getName(), new HashSet<>()).size());
    }

    public int getNotSleeping(@NotNull World world) {
        int notSleepingPlayers = 0;
        for (Player p : world.getPlayers()) {
            if (!this.isSleepingIgnored(p))
                notSleepingPlayers++;
        }
        return notSleepingPlayers;
    }

    @EventHandler
    public void onWorldChangeEvent(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        this.onPlayerWorldChange(p, e.getFrom());
    }

    @EventHandler
    public void onLogin(PlayerJoinEvent e) {
        this.onPlayerWorldChange(e.getPlayer(), null);
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent e) {
        this.onPlayerWorldChange(e.getPlayer(), null);
    }

    private void onPlayerWorldChange(@NotNull Player p, @Nullable World from) {
        if (!this.isSleepingIgnored(p)) {
            ConsoleMessage.debug(this.getClass(), plugin, "Player " + ConsoleMessage.getPlayerString(p) + " switched worlds. Updating...");
            sleepManager.updateWorld(from);
            sleepManager.updateWorld(p.getWorld());
        }
    }
}

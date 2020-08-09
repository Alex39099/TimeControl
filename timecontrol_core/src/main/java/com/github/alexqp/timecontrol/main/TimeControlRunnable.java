package com.github.alexqp.timecontrol.main;

import com.earth2me.essentials.Essentials;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.github.alexqp.timecontrol.data.TimeWorld;
import com.github.alexqp.timecontrol.data.WorldContainer;
import com.github.alexqp.timecontrol.listeners.LeaveBedCounter;

import java.util.*;

public class TimeControlRunnable extends BukkitRunnable {

    static void initialize(JavaPlugin plugin, InternalsProvider internals, long speed, WorldContainer worldContainer, Collection<? extends GameMode> sleepingGameModes, boolean useEssentialsAFK) {
        LeaveBedCounter leaveBedCounter = new LeaveBedCounter();
        Bukkit.getPluginManager().registerEvents(leaveBedCounter, plugin);
        ConsoleMessage.debug(TimeControlRunnable.class, plugin, "Registered LeaveBedCounter");

        new TimeControlRunnable(plugin, internals, speed, worldContainer, sleepingGameModes, useEssentialsAFK, leaveBedCounter);
        ConsoleMessage.debug(TimeControlRunnable.class, plugin, "initialized TimeControlRunnable with speed " + speed);
    }

    private JavaPlugin plugin;
    private InternalsProvider internals;

    private long speed;
    private WorldContainer worldContainer;

    private Collection<? extends GameMode> sleepingGameModes;
    private boolean useEssentialsAFK;

    private LeaveBedCounter leaveBedCounter;

    private Set<String> worldsWithNightSleeping = new HashSet<>();

    private TimeControlRunnable(JavaPlugin plugin, InternalsProvider internals, long speed, WorldContainer worldContainer, Collection<? extends GameMode> sleepingGameModes, boolean useEssentialsAFK, LeaveBedCounter leaveBedCounter) {
        this.plugin = plugin;
        this.internals = internals;
        this.speed = speed;
        this.worldContainer = worldContainer;
        this.sleepingGameModes = sleepingGameModes;
        this.useEssentialsAFK = useEssentialsAFK;
        this.leaveBedCounter = leaveBedCounter;
        this.runTaskTimer(plugin, 0L, speed);
    }

    private boolean checkInstantSleeping(World world) {

        if (leaveBedCounter.didAllNotSleepingIgnoredPlayersLeaveBed(world)) {
            ConsoleMessage.debug(this.getClass(), plugin, "all (none sleepingIgnored) players left the bed at once, making day/clearing thunder...");
            this.makeDayForWorld(world);
            return true;
        }
        return false;
    }

    // returns if world has enough sleeping players...
    private boolean worldHasEnoughSleepingPlayers(World world, TimeWorld tWorld) {
        if (tWorld.getAmountOfSleepingPlayers() > 0) {

            List<Player> sleepingIgnoredPlayers = new ArrayList<>();

            for (Player p : world.getPlayers()) {

                if (sleepingGameModes.contains(p.getGameMode())) {
                    sleepingIgnoredPlayers.add(p);
                    ConsoleMessage.debug(this.getClass(), plugin, "added player " + p.getName() + " as sleepingIgnored over GameMode");
                }
            }

            if (useEssentialsAFK) {
                Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
                if (essentials != null) {
                    for (Player p : world.getPlayers()) {
                        if (essentials.getUser(p).isAfk()) {
                            sleepingIgnoredPlayers.add(p);
                            ConsoleMessage.debug(this.getClass(), plugin, "added player " + p.getName() + " as sleepingIgnored over EssentialsAFK");
                        }
                    }
                } else {
                    ConsoleMessage.send(ConsoleErrorType.ERROR, plugin, "Essentials could not be found despite essentials.afk_is_sleeping was active.");
                    useEssentialsAFK = false;
                }
            }

            sleepingIgnoredPlayers.addAll(internals.getSleepingIgnoredPlayersForWorld(world, "timecontrol.sleepingignored"));

            return tWorld.hasEnoughSleepingPlayers(world.getPlayers().size(), sleepingIgnoredPlayers);
        }

        return false;
    }

    private void makeDayForWorld(World world) {
        TimeWorld tWorld = worldContainer.getTimeWorldForWorld(world);
        internals.setTimeForWorld(world, tWorld.getDayStartTick());
        internals.clearWeatherForWorld(world);
        worldsWithNightSleeping.remove(world.getName());
        ConsoleMessage.debug(this.getClass(), plugin, "made day for world " + world.getName() + " (also clears storms)");
    }

    @Override
    public void run() {
        for (String worldName : worldContainer.getConfigWorldNames()) {
            World world = Bukkit.getWorld(worldName);
            if (world == null || !worldContainer.isEnabled(world)) {
                ConsoleMessage.debug(this.getClass(), plugin, "skipped world " + worldName + " because it was not found/not enabled.");
                continue;
            }

            // checks if all players left the bed at once
            if (checkInstantSleeping(world))
                continue;

            TimeWorld tWorld = worldContainer.getTimeWorldForWorld(world);
            long currentTime = world.getTime();

            long sleepTimeMultiplier = 1;
            int stormTimeSubtrahend = 1;

            if (this.worldHasEnoughSleepingPlayers(world, tWorld)) {
                ConsoleMessage.debug(this.getClass(), plugin, "World " + worldName + " has enough sleeping players");

                if (!tWorld.isDay(currentTime)) {
                    sleepTimeMultiplier = tWorld.getSleepTimeMultiplier();
                    worldsWithNightSleeping.add(worldName);

                    if (sleepTimeMultiplier == 0) {
                        ConsoleMessage.debug(this.getClass(), plugin, "sleepTimeMultiplier is 0 which indicates instant day...");
                        this.makeDayForWorld(world);
                        continue;
                    }
                } else {
                    stormTimeSubtrahend = tWorld.getStormTimeSubtrahend();

                    if (stormTimeSubtrahend == -1) {
                        ConsoleMessage.debug(this.getClass(), plugin, "stormTimeSubtrahend is -1 which indicates instant day (normal MC behaviour)...");
                        this.makeDayForWorld(world);
                        continue;
                    }
                }
            }

            // ASSERT
            // sleepTimeMultiplier == 1 if day
            // stormTimeSubtrahend == 0 if night
            // stormTimeSubtrahend != 0 if day and enough sleeping players

            double addTime;

            if (tWorld.isDay(currentTime)) {
                addTime = (double) tWorld.getDefaultDayLength() / tWorld.getDayLength() * this.speed;
            } else {
                double defaultNightLength = 24000 - tWorld.getDefaultDayLength();
                addTime = defaultNightLength / tWorld.getNightLength() * this.speed;
            }

            long updatedTime = currentTime + (long) (addTime * sleepTimeMultiplier);

            if (updatedTime >= 24000) {
                updatedTime = updatedTime % 24000;
                ConsoleMessage.debug(this.getClass(), plugin, "readjust updatedTime to " + updatedTime + " because it was >= 24000");
            }

            if (tWorld.isDay(updatedTime) && worldsWithNightSleeping.remove(worldName)) {
                ConsoleMessage.debug(this.getClass(), plugin, "making day for world " + worldName + " bc there was night sleeping");
                this.makeDayForWorld(world);
                continue;
            }

            if (world.hasStorm() && tWorld.isDay(currentTime) && stormTimeSubtrahend != 1) {
                ConsoleMessage.debug(this.getClass(), plugin, "There is storm for world " + worldName + " at daytime. Speeding up storm...");
                ConsoleMessage.debug(this.getClass(), plugin, "WeatherDuration = " + world.getWeatherDuration());
                internals.adjustWeatherByDivisorForWorld(world, stormTimeSubtrahend);
                ConsoleMessage.debug(this.getClass(), plugin, "Updated WeatherDuration = " + world.getWeatherDuration());
                continue;
            }

            // time is forwarding because of sleeping and passed day-start-tick
            if (sleepTimeMultiplier != 1 && tWorld.isDay(updatedTime)) {
                ConsoleMessage.debug(this.getClass(), plugin, "passed day-start-tick while sleeping, making day...");
                this.makeDayForWorld(world);
                continue;
            }

            internals.setTimeForWorld(world, updatedTime);
        }
    }
}

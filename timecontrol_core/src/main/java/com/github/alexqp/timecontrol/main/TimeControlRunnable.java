package com.github.alexqp.timecontrol.main;

import com.github.alexqp.commons.messages.ConsoleMessage;
import com.github.alexqp.timecontrol.sleep_mechanic.SleepManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.github.alexqp.timecontrol.data.TimeWorld;
import com.github.alexqp.timecontrol.data.WorldContainer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class TimeControlRunnable extends BukkitRunnable {

    private static JavaPlugin plugin;
    private static WorldContainer worldContainer;
    private static SleepManager sleepManager;

    private static final HashMap<String, TimeControlRunnable> runningTimers = new HashMap<>();

    static void initialize(@NotNull JavaPlugin plugin, int speed, @NotNull WorldContainer worldContainer, @NotNull SleepManager sleepManager) {
        TimeControlRunnable.plugin = plugin;
        TimeControlRunnable.worldContainer = worldContainer;
        TimeControlRunnable.sleepManager = sleepManager;

        new TimeControlRunnable(speed);
        ConsoleMessage.debug(TimeControlRunnable.class, plugin, "initialized TimeControlRunnable with speed " + speed);
    }

    private final int speed;

    private TimeControlRunnable(int speed) {
        this.speed = speed;
        this.runTaskTimer(plugin, 0L, speed);
    }

    private void makeDayForWorld(World world, TimeWorld tWorld) {
        world.setTime(tWorld.getDayStartTick());
        this.setWeather(world, -1);
        sleepManager.setSleeping(world, false);
        ConsoleMessage.debug(this.getClass(), plugin, "made day for world " + world.getName() + ". (also clears storms)");
    }

    private boolean isStormyWorld(@NotNull World world) {
        return world.hasStorm() || world.isThundering();
    }

    @Override
    public void run() {
        for (String worldName : worldContainer.getLoadedWorlds()) {
            World world = Bukkit.getWorld(worldName);
            if (!worldContainer.isEnabled(world)) {
                continue;
            }

            assert world != null;
            TimeWorld tWorld = worldContainer.getTimeWorldForWorld(world);
            assert tWorld != null;
            long currentTime = world.getTime();
            int stormTime = world.getWeatherDuration();

            int timeMultiplier = 1;
            int stormMultiplier = 0;

            if (sleepManager.isSleepingWorld(world)) {

                // night behaviour
                if (!tWorld.isDay(currentTime)) {
                    timeMultiplier = tWorld.getNightTimeMultiplier();
                }

                // storm behaviour
                if (this.isStormyWorld(world)) {
                    stormMultiplier = tWorld.getStormTimeMultiplier();
                }

                if (timeMultiplier == -1 || stormMultiplier == -1) {
                    ConsoleMessage.debug(this.getClass(), plugin, "Skipping of night or storm was set to vanilla behaviour while sleeping. Making day...");
                    this.makeDayForWorld(world, tWorld);
                    return;
                }
            }

            double addTime;
            if (tWorld.isDay(currentTime)) {
                addTime = (double) tWorld.getDefaultDayLength() / tWorld.getDayLength();
            } else {
                double defaultNightLength = 24000 - tWorld.getDefaultDayLength();
                addTime = defaultNightLength / tWorld.getNightLength() ;
            }
            addTime = addTime * this.speed * timeMultiplier;

            int updatedTime = (int) Math.ceil(currentTime + addTime);
            if (updatedTime >= 24000) {
                updatedTime = updatedTime % 24000;
                ConsoleMessage.debug(this.getClass(), plugin, "readjust updatedTime to " + updatedTime + " because it was >= 24000");
            }

            if (sleepManager.isSleepingWorld(world) && !tWorld.isDay(currentTime) && tWorld.isDay(updatedTime)) {
                this.makeDayForWorld(world, tWorld);
                return;
            }

            world.setTime(updatedTime);

            if (stormMultiplier > 0) {
                this.setWeather(world,  stormTime - (int) Math.ceil(this.speed * stormMultiplier) + this.speed); // weather gameRule is not deactivated.
            }

        }
    }

    private void setWeather(@NotNull World world, int duration) {
        if (duration <= 0) {
            world.setStorm(false);
            world.setThundering(false);
            ConsoleMessage.debug(this.getClass(), plugin, "Cleared weather.");
            return;
        }
        world.setWeatherDuration(duration);
        world.setThunderDuration(duration);
        ConsoleMessage.debug(this.getClass(), plugin, "Adjusted weather duration. New Duration = " + duration);
    }
}

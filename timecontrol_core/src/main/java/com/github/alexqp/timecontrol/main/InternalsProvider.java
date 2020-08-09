package com.github.alexqp.timecontrol.main;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class InternalsProvider {

    public InternalsProvider() {}

    public boolean disableDayLightCycle(World world) {
        return world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
    }

    public boolean setDayLightCycle(World world, boolean value) {
        Boolean gameRuleValue = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
        if (gameRuleValue != null && gameRuleValue.equals(value)) {
            return false;
        }
        return world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, value);
    }

    public List<Player> getSleepingIgnoredPlayersForWorld(World world, String sleepingIgnoredPermission) {
        List<Player> list = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.isSleepingIgnored() || p.hasPermission(sleepingIgnoredPermission)) // TODO hier eig isSleepingIgnored nicht mehr nötig wegen leaveBedCounter...
                list.add(p);
        }
        return list;
    }

    public void setTimeForWorld(World world, long time) {
        world.setTime(time);
    }

    public void clearWeatherForWorld(World world) {
        world.setStorm(false);
        world.setThundering(false);
    }

    public void adjustWeatherByDivisorForWorld(World world, int subtrahend) {
        int weatherDuration = world.getWeatherDuration() - subtrahend;
        if (weatherDuration <= 0) {
            this.clearWeatherForWorld(world);
            return;
        }
        world.setWeatherDuration(weatherDuration);
    }
}

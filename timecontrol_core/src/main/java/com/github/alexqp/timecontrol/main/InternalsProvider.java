package com.github.alexqp.timecontrol.main;

import org.bukkit.GameRule;
import org.bukkit.World;

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
}

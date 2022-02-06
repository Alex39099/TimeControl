package com.github.alexqp.timecontrol.main;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class InternalsProvider {

    public InternalsProvider() {}

    public boolean handleGameRules(@NotNull World world, boolean enable) {
        world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, enable ? 101 : 100);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, enable);
        return true;
    }

    public boolean handleGameRules(@NotNull World world) {
        return this.handleGameRules(world, false);
    }
}

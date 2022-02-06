package com.github.alexqp.timecontrol.main;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class v1_16_R2 extends InternalsProvider {

    public boolean handleGameRules(@NotNull World world, boolean enable) {
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, enable);
        return true;
    }
}

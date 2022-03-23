package com.github.alexqp.timecontrol.main;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class v1_16_R1 extends InternalsProvider {

    public boolean handleGameRules(@NotNull World world, boolean enable) {
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, enable);
        return true;
    }

    public boolean needAllLeftBedTrigger() {
        return true;
    }

    @Override
    public void disableSleepActionBar(@NotNull JavaPlugin plugin) {
    }
}

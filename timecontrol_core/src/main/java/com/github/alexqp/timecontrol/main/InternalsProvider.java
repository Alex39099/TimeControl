package com.github.alexqp.timecontrol.main;

import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class InternalsProvider {

    public InternalsProvider() {}

    public boolean handleGameRules(@NotNull World world) {
        return this.handleGameRules(world, false);
    }

    public boolean handleGameRules(@NotNull World world, boolean enable) {
        world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, enable ? 100 : 101);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, enable);
        return true;
    }

    public boolean needAllLeftBedTrigger() {
        return false;
    }

    public void disableSleepActionBar(@NotNull JavaPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            ProtocolLib.disableSleepActionBar(plugin);
        } else {
            ConsoleMessage.send(ConsoleErrorType.WARN, plugin, "ProtocolLib is not installed on your server. Vanilla-Sleep-Messages will not be disabled");
        }
    }
}

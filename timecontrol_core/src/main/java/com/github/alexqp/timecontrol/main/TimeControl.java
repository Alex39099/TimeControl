package com.github.alexqp.timecontrol.main;

import com.github.alexqp.timecontrol.command.TimeControlCommand;
import com.github.alexqp.timecontrol.sleep_mechanic.SleepManager;
import com.google.common.collect.Range;
import com.github.alexqp.commons.bstats.bukkit.Metrics;
import com.github.alexqp.commons.config.ConfigChecker;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;
import com.github.alexqp.commons.messages.Debugable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.github.alexqp.timecontrol.data.TimeWorld;
import com.github.alexqp.timecontrol.data.WorldContainer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

public class TimeControl extends JavaPlugin implements Debugable {

    /*
     * Changelog v4.0.0+:
     *
     */

    private boolean debug = false;

    @Override
    public boolean getDebug() {
        return debug;
    }

    private void checkDebugMode() {
        String configName = "debug";
        if (this.getConfig().contains(configName) && this.getConfig().isBoolean(configName) && this.getConfig().getBoolean(configName)) {
            debug = true;
            ConsoleMessage.debug((Debugable) this, "DebugMode enabled.");
        }
    }

    private static InternalsProvider internals;
    static {
        try {
            String packageName = TimeControl.class.getPackage().getName();
            String internalsName = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            internals = (InternalsProvider) Class.forName(packageName + "." + internalsName).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException | NoSuchMethodException | InvocationTargetException exception) {
            Bukkit.getLogger().log(Level.SEVERE, "TimeControl could not find a valid implementation for this server version.");
            internals = new InternalsProvider();
        }
    }

    static {
        ConfigurationSerialization.registerClass(TimeWorld.class, "TimeWorld");
    }

    private WorldContainer worldContainer;

    @Override
    public void onEnable() {
        new Metrics(this); // id 3195
        this.getLogger().info("This plugin was made by alex_qp.");
        new BukkitRunnable() {
            @Override
            public void run() {
                onRealEnable();
            }
        }.runTask(this);
    }

    public void onReload() {
        ConsoleMessage.debug((Debugable) this, "Reload command was executed");
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
        worldContainer.save();
        this.onRealEnable();
    }

    @Override
    public void onDisable() {
        if (worldContainer != null)
            worldContainer.save();
    }

    private void onRealEnable() {
        this.saveDefaultConfig();
        this.reloadConfig();
        ConfigChecker configChecker = new ConfigChecker(this);

        this.checkDebugMode();

        if (!this.checkConfigState(configChecker)) {
            this.onDisable();
            return;
        }

        worldContainer = WorldContainer.build(this, internals, this.getDefTimeWorld());
        SleepManager sleepManager = SleepManager.build(this, worldContainer);

        int delay = configChecker.checkInt(this.getConfig(), "check-delay", ConsoleErrorType.WARN, 5, Range.greaterThan(0));
        TimeControlRunnable.initialize(this, delay, worldContainer, sleepManager);

        sleepManager.setMessengerPrefix(new TimeControlCommand(this, internals, worldContainer).getPrefix());
    }

    private boolean checkConfigState(@NotNull ConfigChecker configChecker) {
        String isConfiguredConfigName = "configured";
        if (!configChecker.checkBoolean(this.getConfig(), isConfiguredConfigName, ConsoleErrorType.ERROR, false)) {
            ConsoleMessage.send(ConsoleErrorType.ERROR, this, "Plugin was not yet configured. Please configure the config.yml and set " + isConfiguredConfigName + " to true. Restart the server afterwards.");
            return false;
        }
        return true;
    }

    private TimeWorld getDefTimeWorld() {
        ConfigChecker configChecker = new ConfigChecker(this);
        String defTimeWorldConfigName = "default_world_settings";
        ConfigurationSection section = configChecker.checkConfigSection(this.getConfig(), defTimeWorldConfigName, ConsoleErrorType.ERROR);
        TimeWorld tWorld = new TimeWorld();
        if (section != null) {
            tWorld = TimeWorld.deserialize(section.getValues(false));
        }
        tWorld.checkValues(configChecker, this.getConfig(), defTimeWorldConfigName, ConsoleErrorType.WARN,true);
        return tWorld;
    }
}

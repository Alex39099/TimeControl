package com.github.alexqp.timecontrol.main;

import com.google.common.collect.Range;
import com.github.alexqp.commons.bstats.Metrics;
import com.github.alexqp.commons.config.ConfigChecker;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;
import com.github.alexqp.commons.messages.Debugable;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.github.alexqp.timecontrol.command.TimeControlCommand;
import com.github.alexqp.timecontrol.data.TimeWorld;
import com.github.alexqp.timecontrol.data.WorldContainer;
import com.github.alexqp.timecontrol.listeners.BedListener;

import java.util.*;
import java.util.logging.Level;

public class TimeControl extends JavaPlugin implements Debugable {

    /*
     * Changelog v3.6.2:
     *
     * Fixed: some default values in the config message section.
     *
     * // TODO Added: sleeping players will now be informed by the plugin if they enter the bed how many players are needed.... (permission)
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
            internals = (InternalsProvider) Class.forName(packageName + "." + internalsName).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException exception) {
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
        new Metrics(this);
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
        worldContainer.saveAllChangedTimeWorlds();
        this.onRealEnable();
    }

    @Override
    public void onDisable() {
        if (worldContainer != null)
            worldContainer.saveAllChangedTimeWorlds();
    }

    private void onRealEnable() {
        this.saveDefaultConfig();
        this.checkDebugMode();
        ConfigChecker configChecker = new ConfigChecker(this);

        // already configured?
        String isConfiguredConfigName = "configured";
        if (!configChecker.checkBoolean(this.getConfig(), isConfiguredConfigName, ConsoleErrorType.ERROR, false)) {
            ConsoleMessage.send(ConsoleErrorType.ERROR, this, "Plugin was not yet configured. Please configure the config.yml and set " + isConfiguredConfigName + " to true. Restart the server afterwards.");
            this.onDisable();
            return;
        }

        long delay = configChecker.checkLong(this.getConfig(), "check-delay", ConsoleErrorType.WARN, 5, Range.greaterThan(0L));
        boolean deactivateEmptyWorlds = configChecker.checkBoolean(this.getConfig(), "deactivate_empty_worlds", ConsoleErrorType.WARN, true);

        List<World> configWorlds = new ArrayList<>();
        ConfigurationSection section = configChecker.checkConfigSection(this.getConfig(), "world_exceptions", ConsoleErrorType.ERROR);
        if (section != null) {
            List<String> list = section.getStringList("by_name");
            List<World.Environment> envList = this.getWorldEnvList(section);

            for (World world : Bukkit.getWorlds()) {
                if (!list.contains(world.getName()) && !envList.contains(world.getEnvironment())) {
                    ConsoleMessage.debug((Debugable) this, "added world " + world.getName() + " to configWorlds.");
                    configWorlds.add(world);
                } else {
                    ConsoleMessage.debug((Debugable) this, "did not add " + world.getName() + " to configWorlds.");
                }
            }
        }

        worldContainer = new WorldContainer(this, internals, configWorlds, deactivateEmptyWorlds, this.getDefTimeWorld());

        section = configChecker.checkConfigSection(this.getConfig(), "gamemode_sleeping", ConsoleErrorType.ERROR);
        Set<GameMode> sleepingGameModes = new HashSet<>();
        if (section != null) {
            for (GameMode gameMode : GameMode.values()) {
                if (configChecker.checkBoolean(section, gameMode.name().toLowerCase(), ConsoleErrorType.WARN, false)) {
                    sleepingGameModes.add(gameMode);
                }
            }
        }

        boolean useEssentialsAFK = false;
        section = configChecker.checkConfigSection(this.getConfig(), "essentials", ConsoleErrorType.ERROR);
        if (section != null) {
            useEssentialsAFK = configChecker.checkBoolean(section, "afk_is_sleeping", ConsoleErrorType.WARN, false);
        }


        TimeControlRunnable.initialize(this, internals, delay, worldContainer, sleepingGameModes, useEssentialsAFK);

        TimeControlCommand command = new TimeControlCommand(this, internals, worldContainer);
        PluginCommand pluginCommand = this.getCommand(command.getName());
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
            ConsoleMessage.debug((Debugable) this, "set executor and tabCompleter for command");
        } else {
            ConsoleMessage.send(ConsoleErrorType.ERROR, this, "Please contact developer with error: javaPlugin#getCommand returned null for commandName + " + command.getName());
        }

        BedListener bedListener = new BedListener(this, worldContainer);
        Bukkit.getPluginManager().registerEvents(bedListener ,this);
        ConsoleMessage.debug((Debugable) this, "registered new BedListener");
    }

    private List<World.Environment> getWorldEnvList(ConfigurationSection section) {
        List<World.Environment> list = new ArrayList<>();

        ConfigChecker configChecker = new ConfigChecker(this);
        ConfigurationSection envSection = configChecker.checkConfigSection(section, "by_environment", ConsoleErrorType.ERROR);
        if (envSection != null) {
            for (World.Environment worldEnv : World.Environment.values()) {
                if (configChecker.checkBoolean(envSection, worldEnv.name().toLowerCase(), ConsoleErrorType.WARN, false))
                    list.add(worldEnv);
            }
        }
        return list;
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
        TimeWorld.setDefTimeWorld(tWorld);
        return tWorld;
    }


}

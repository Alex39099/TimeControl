package com.github.alexqp.timecontrol.data;

import com.github.alexqp.commons.config.ConfigChecker;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.dataHandler.DataHandler;
import com.github.alexqp.commons.dataHandler.LoadSaveException;
import com.github.alexqp.commons.messages.ConsoleMessage;

import com.github.alexqp.timecontrol.main.TimeControl;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import com.github.alexqp.timecontrol.main.InternalsProvider;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WorldContainer implements Listener {

    public static WorldContainer build(@NotNull JavaPlugin plugin, @NotNull InternalsProvider internals, @NotNull TimeWorld defTimeWorld) {
        ConfigChecker configChecker = new ConfigChecker(plugin);
        boolean deactivateEmptyWorlds = configChecker.checkBoolean(plugin.getConfig(), "deactivate_empty_worlds", ConsoleErrorType.WARN, true);
        HashSet<World.Environment> disabledEnvironments = new HashSet<>();
        HashSet<String> disabledNames = new HashSet<>();

        ConfigurationSection worldExceptionSection = configChecker.checkConfigSection(plugin.getConfig(), "world_exceptions", ConsoleErrorType.ERROR);
        if (worldExceptionSection != null) {
            disabledNames.addAll(worldExceptionSection.getStringList("by_name"));
            ConfigurationSection envSection = configChecker.checkConfigSection(worldExceptionSection, "by_environment", ConsoleErrorType.ERROR);
            if (envSection != null) {
                for (World.Environment worldEnv : World.Environment.values()) {
                    if (configChecker.checkBoolean(envSection, worldEnv.name().toLowerCase(), ConsoleErrorType.WARN, false))
                        disabledEnvironments.add(worldEnv);
                }
            }
        }
        return new WorldContainer(internals, deactivateEmptyWorlds, disabledEnvironments, disabledNames, defTimeWorld);
    }

    private final JavaPlugin plugin;
    private final InternalsProvider internals;
    private final String worldConfigFileName = "worldConfigurations";

    private final Map<String, TimeWorld> loadedTimeWorlds = new HashMap<>();
    private final boolean deactivateEmptyWorlds;

    private final HashSet<World.Environment> disabledEnvironments;
    private final HashSet<String> disabledNames;

    private WorldContainer(@NotNull InternalsProvider internals,
                               boolean deactivateEmptyWorlds, @NotNull HashSet<World.Environment> disabledEnvironments, @NotNull HashSet<String> disabledNames,
                               @NotNull TimeWorld defTimeWorld) {
        this.plugin = TimeControl.getInstance();
        this.internals = internals;
        this.deactivateEmptyWorlds = deactivateEmptyWorlds;
        this.disabledEnvironments = disabledEnvironments;
        this.disabledNames = disabledNames;
        TimeWorld.setDefValues(defTimeWorld);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.load();
    }

    private void load() {
        for (World world : Bukkit.getWorlds()) {
            this.load(world.getName());
        }
    }

    @Nullable
    private TimeWorld load(@NotNull String worldName) {
        ConsoleMessage.debug(this.getClass(), plugin, "Trying to load tWorld for world " + worldName + "...");
        if (loadedTimeWorlds.containsKey(worldName)) {
            ConsoleMessage.debug(this.getClass(), plugin, "Skipped loading for world " + worldName + " (already loaded)");
            return loadedTimeWorlds.get(worldName);
        }
        if (disabledNames.contains(worldName)) {
            ConsoleMessage.debug(this.getClass(), plugin, "Skipped loading for world " + worldName + " (disabled by name)");
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null || disabledEnvironments.contains(world.getEnvironment())) {
            ConsoleMessage.debug(this.getClass(), plugin, "Skipped loading for world " + worldName + " (not Bukkit-loaded or disabled by environment)");
            return null;
        }

        DataHandler dataHandler = new DataHandler(plugin);
        YamlConfiguration ymlFile = dataHandler.loadYmlFile(worldConfigFileName);
        ConfigChecker configChecker = new ConfigChecker(plugin, ymlFile);

        TimeWorld tWorld = new TimeWorld();
        if (ymlFile.contains(worldName)) {
            tWorld = configChecker.checkSerializable(ymlFile, worldName, ConsoleErrorType.WARN, new TimeWorld(), true);
            ConsoleMessage.debug(this.getClass(), plugin, "Loaded world " + worldName + " of file.");
        } else {
            ConsoleMessage.debug(this.getClass(), plugin, "Loaded new data for world " + worldName + " and disabling gameRules");
            tWorld.setChanged(true);
        }
        internals.handleGameRules(world); // has to be put outside because new gameRule is modified since v4.2.0
        loadedTimeWorlds.put(worldName, tWorld);
        return tWorld;
    }

    private void saveYmlFile(DataHandler dataHandler, YamlConfiguration ymlFile) {
        try {
            dataHandler.saveYmlFile(worldConfigFileName, ymlFile);
            ConsoleMessage.debug(this.getClass(), plugin, "saved " + worldConfigFileName);
        } catch (LoadSaveException e) {
            ConsoleMessage.send(ConsoleErrorType.ERROR, plugin, worldConfigFileName, "file", "File could not be saved. Please check writing ability of directory");
            e.printStackTrace();
        }
    }

    public void save() {
        DataHandler dataHandler = new DataHandler(plugin);
        YamlConfiguration ymlFile = dataHandler.loadYmlFile(worldConfigFileName);
        boolean saveYmlFile = false;
        for (String worldName : loadedTimeWorlds.keySet()) {
            TimeWorld tWorld = loadedTimeWorlds.get(worldName);
            if (tWorld.isChanged()) {
                tWorld.setChanged(false);
                ymlFile.set(worldName, tWorld);
                saveYmlFile = true;
                ConsoleMessage.debug(this.getClass(), plugin, "updated TimeWorld for world " + worldName + " in order to be saved.");
            }
        }
        if (saveYmlFile)
            this.saveYmlFile(dataHandler, ymlFile);
    }

    public boolean isLoaded(@Nullable World world) {
        if (world != null)
            return this.loadedTimeWorlds.containsKey(world.getName());
        return false;
    }

    public boolean isEnabled(@Nullable World world) {
        if (world != null)
            return !(deactivateEmptyWorlds && world.getPlayers().isEmpty());
        return false;
    }

    public Set<String> getLoadedWorlds() {
        return this.loadedTimeWorlds.keySet();
    }

    // this should load the data if not existent
    @Nullable public TimeWorld getTimeWorldForWorld(@NotNull World world) {
        return this.getTimeWorldForWorld(world.getName());
    }

    @Nullable public TimeWorld getTimeWorldForWorld(@NotNull String worldName) {
        return this.load(worldName);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        this.load(e.getWorld().getName());
    }
}

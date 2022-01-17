package com.github.alexqp.timecontrol.data;

import com.github.alexqp.commons.config.ConfigChecker;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.dataHandler.DataHandler;
import com.github.alexqp.commons.dataHandler.LoadSaveException;
import com.github.alexqp.commons.messages.ConsoleMessage;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import com.github.alexqp.timecontrol.main.InternalsProvider;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WorldContainer {

    public static WorldContainer build(@NotNull JavaPlugin plugin, @NotNull InternalsProvider internals, @NotNull TimeWorld defTimeWorld) {
        ConfigChecker configChecker = new ConfigChecker(plugin);
        boolean deactivateEmptyWorlds = configChecker.checkBoolean(plugin.getConfig(), "deactivate_empty_worlds", ConsoleErrorType.WARN, true);

        List<World> configWorlds = new ArrayList<>();
        ConfigurationSection worldExceptionSection = configChecker.checkConfigSection(plugin.getConfig(), "world_exceptions", ConsoleErrorType.ERROR);
        if (worldExceptionSection != null) {
            List<String> excludedWorlds = worldExceptionSection.getStringList("by_name");

            List<World.Environment> envList = new ArrayList<>();
            ConfigurationSection envSection = configChecker.checkConfigSection(worldExceptionSection, "by_environment", ConsoleErrorType.ERROR);
            if (envSection != null) {
                for (World.Environment worldEnv : World.Environment.values()) {
                    if (configChecker.checkBoolean(envSection, worldEnv.name().toLowerCase(), ConsoleErrorType.WARN, false))
                        envList.add(worldEnv);
                }
            }

            for (World world : Bukkit.getWorlds()) {
                if (!excludedWorlds.contains(world.getName()) && !envList.contains(world.getEnvironment())) {
                    ConsoleMessage.debug(WorldContainer.class, plugin, "added world " + world.getName() + " to configWorlds.");
                    configWorlds.add(world);
                } else {
                    ConsoleMessage.debug(WorldContainer.class, plugin, "did not add " + world.getName() + " to configWorlds.");
                }
            }
        }
        return new WorldContainer(plugin, internals, configWorlds, deactivateEmptyWorlds, defTimeWorld);
    }

    private final JavaPlugin plugin;
    private final String worldConfigFileName = "worldConfigurations";

    private final Map<String, TimeWorld> configWorldMap = new HashMap<>();
    private final boolean deactivateEmptyWorlds;

    private WorldContainer(@NotNull JavaPlugin plugin, @NotNull InternalsProvider internals, @NotNull List<World> configWorlds, boolean deactivateEmptyWorlds, @NotNull TimeWorld defTimeWorld) {
        this.plugin = plugin;
        this.deactivateEmptyWorlds = deactivateEmptyWorlds;
        TimeWorld.setDefValues(defTimeWorld);
        this.loadConfigWorldMap(internals, configWorlds);
    }

    private void loadConfigWorldMap(@NotNull InternalsProvider internals, @NotNull List<World> configWorlds) {
        DataHandler dataHandler = new DataHandler(plugin);
        YamlConfiguration ymlFile = dataHandler.loadYmlFile(worldConfigFileName);
        ConsoleMessage.debug(this.getClass(), plugin, "YMLFILE KEYSET = " + ymlFile.getKeys(true).toString());
        boolean saveYmlFile = false;

        ConfigChecker configChecker = new ConfigChecker(plugin, ymlFile);

        for (World world : configWorlds) {
            TimeWorld tWorld = new TimeWorld();
            String worldName = world.getName();
            ConsoleMessage.debug(this.getClass(), plugin, "Trying to load tWorld for world " + worldName + "...");
            if (!ymlFile.contains(worldName)) {
                ConsoleMessage.debug(this.getClass(), plugin, "added world " + worldName + " to " + worldConfigFileName + " because it was not existent.");
                ymlFile.set(worldName, new TimeWorld());
                saveYmlFile = true;
            } else {
                TimeWorld checkerDefTimeWorld = new TimeWorld();
                tWorld = configChecker.checkSerializable(ymlFile, worldName, ConsoleErrorType.WARN, checkerDefTimeWorld, true);

                if (tWorld == checkerDefTimeWorld) {
                    ConsoleMessage.debug(this.getClass(), plugin, "Could not load tWorld for world " + worldName + " from file (corrupt data)");
                    ymlFile.set(worldName, new TimeWorld());
                    saveYmlFile = true;
                } else {
                    ConsoleMessage.debug(this.getClass(), plugin, "Loaded tWorld for world " + worldName + " from file");
                }
            }

            Boolean gameRuleValue = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
            if (gameRuleValue == null || gameRuleValue) {
                if (internals.disableDayLightCycle(world)) {
                    plugin.getLogger().info("disabled " + GameRule.DO_DAYLIGHT_CYCLE.getName() + " for world " + worldName);
                } else {
                    ConsoleMessage.send(ConsoleErrorType.ERROR, plugin, worldConfigFileName, worldName, "game rule could not be disabled. (not loaded world)");
                    continue;
                }
            }
            configWorldMap.put(worldName, tWorld);
        }

        if (saveYmlFile) {
            this.saveYmlFile(dataHandler, ymlFile);
        }
    }

    private void saveYmlFile(DataHandler dataHandler, YamlConfiguration ymlFile) {
        try {
            dataHandler.saveYmlFile(worldConfigFileName, ymlFile);
            ConsoleMessage.debug(this.getClass(), plugin, "saved " + worldConfigFileName);
        } catch (LoadSaveException e) {
            ConsoleMessage.send(ConsoleErrorType.ERROR, plugin, worldConfigFileName, "file", "File could not be saved. Please check writing ability of directory");
        }
    }

    public void save() {
        DataHandler dataHandler = new DataHandler(plugin);
        YamlConfiguration ymlFile = dataHandler.loadYmlFile(worldConfigFileName);
        boolean saveYmlFile = false;
        for (String worldName : configWorldMap.keySet()) {
            TimeWorld tWorld = configWorldMap.get(worldName);
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

    public boolean isConfigEnabled(@Nullable World world) {
        if (world != null)
            return this.configWorldMap.containsKey(world.getName());
        return false;
    }

    public boolean isEnabled(@Nullable World world) {
        if (world != null)
            return !(deactivateEmptyWorlds && world.getPlayers().isEmpty());
        return false;
    }

    public Set<String> getConfigWorldNames() {
        return this.configWorldMap.keySet();
    }

    @NotNull public TimeWorld getTimeWorldForWorld(@NotNull World world) {
        return this.getTimeWorldForWorld(world.getName());
    }

    @NotNull public TimeWorld getTimeWorldForWorld(@NotNull String worldName) {
        TimeWorld tWorld = configWorldMap.get(worldName);
        if (tWorld == null) {
            tWorld = new TimeWorld();
            tWorld.setChanged(true);
            configWorldMap.put(worldName, tWorld);
            ConsoleMessage.debug(this.getClass(), plugin, "could not find TimeWorld for world " + worldName + ". Returned defTimeWorld and put data in map");
        }
        return tWorld;
    }
}

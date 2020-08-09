package com.github.alexqp.timecontrol.data;

import com.github.alexqp.commons.config.ConfigChecker;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.dataHandler.DataHandler;
import com.github.alexqp.commons.dataHandler.LoadSaveException;
import com.github.alexqp.commons.messages.ConsoleMessage;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import com.github.alexqp.timecontrol.main.InternalsProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorldContainer {

    private JavaPlugin plugin;

    private String worldConfigFileName = "worldConfigurations";

    private Map<String, TimeWorld> configWorldMap = new HashMap<>();
    private boolean deactivateEmptyWorlds;
    private TimeWorld defTimeWorld;

    public WorldContainer(JavaPlugin plugin, InternalsProvider internals, List<World> configWorlds, boolean deactivateEmptyWorlds, TimeWorld defTimeWorld) {
        this.plugin = plugin;
        this.deactivateEmptyWorlds = deactivateEmptyWorlds;
        this.defTimeWorld = defTimeWorld;
        this.loadConfigWorldMap(internals, configWorlds);
    }

    public boolean isConfigEnabled(World world) {
        if (world != null)
            return this.configWorldMap.containsKey(world.getName());
        return false;
    }
    public boolean isEnabled(World world) {
        if (world != null)
            return !(deactivateEmptyWorlds && world.getPlayers().isEmpty());
        return false;
    }

    public Set<String> getConfigWorldNames() {
        return this.configWorldMap.keySet();
    }

    private void loadConfigWorldMap(InternalsProvider internals, List<World> configWorlds) {
        DataHandler dataHandler = new DataHandler(plugin);
        YamlConfiguration ymlFile = dataHandler.loadYmlFile(worldConfigFileName);
        ConsoleMessage.debug(this.getClass(), plugin, "YMLFILE KEYSET = " + ymlFile.getKeys(true).toString());
        boolean saveYmlFile = false;

        ConfigChecker configChecker = new ConfigChecker(plugin, ymlFile);

        for (World world : configWorlds) {
            TimeWorld tWorld = new TimeWorld(defTimeWorld);
            String worldName = world.getName();
            ConsoleMessage.debug(this.getClass(), plugin, "Trying to load tWorld for world " + worldName + "...");
            if (!ymlFile.contains(worldName)) {
                ConsoleMessage.debug(this.getClass(), plugin, "added world " + worldName + " to " + worldConfigFileName + " because it was not existent.");
                ymlFile.set(worldName, new TimeWorld(defTimeWorld));
                saveYmlFile = true;
            } else {
                TimeWorld checkerDefTimeWorld = new TimeWorld(defTimeWorld);
                tWorld = configChecker.checkSerializable(ymlFile, worldName, ConsoleErrorType.WARN, checkerDefTimeWorld, true);

                if (tWorld == checkerDefTimeWorld) {
                    ConsoleMessage.debug(this.getClass(), plugin, "Could not load tWorld for world " + worldName + " from file (corrupt data)");
                    ymlFile.set(worldName, new TimeWorld(defTimeWorld));
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
                    ConsoleMessage.send(ConsoleErrorType.ERROR, plugin, worldConfigFileName, worldName, "game rule could not be disabled. (deactivated world)");
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

    @NotNull public TimeWorld getTimeWorldForWorld(World world) {
        return this.getTimeWorldForWorld(world.getName());
    }

    @NotNull public TimeWorld getTimeWorldForWorld(String worldName) {
        TimeWorld tWorld = configWorldMap.get(worldName);
        if (tWorld == null) {
            tWorld = new TimeWorld(defTimeWorld);
            tWorld.setChanged(true);
            configWorldMap.put(worldName, tWorld);
            ConsoleMessage.debug(this.getClass(), plugin, "could not find TimeWorld for world " + worldName + ". Returned defTimeWorld and put data in map");
        }
        return tWorld;
    }

    public void saveAllChangedTimeWorlds() {
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
}

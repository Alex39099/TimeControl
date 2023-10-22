package com.github.alexqp.timecontrol.main;

import com.github.alexqp.timecontrol.command.TimeControlCommand;
import com.github.alexqp.timecontrol.sleep_mechanic.SleepManager;
import com.google.common.collect.Range;
import com.github.alexqp.commons.bstats.bukkit.Metrics;
import com.github.alexqp.commons.config.ConfigChecker;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;
import com.github.alexqp.commons.messages.Debugable;
import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class TimeControl extends JavaPlugin implements Debugable {

    /*
     * Changelog v4.3.0:
     *
     * Added: official support for MC 1.19.
     *
     * Fixed: NPE if custom login messages are used.
     * Fixed: NoClassDefFoundError if ProtocolLib was not installed.
     */

    private static final String defaultInternalsVersion = "v1_20_R2";
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
            String internalsName = getInternalsName(Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]);
            if (defaultInternalsVersion.equals(internalsName)) {
                Bukkit.getLogger().log(Level.INFO, TimeControl.class.getSimpleName() + " is using the latest implementation (last tested for " + defaultInternalsVersion + ").");
                internals = new InternalsProvider();
            } else {
                Bukkit.getLogger().log(Level.INFO, TimeControl.class.getSimpleName() + " is using the implementation for version " + internalsName + ".");
                internals = (InternalsProvider) Class.forName(packageName + "." + internalsName).getDeclaredConstructor().newInstance();
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException | NoSuchMethodException | InvocationTargetException exception) {
            Bukkit.getLogger().log(Level.WARNING, TimeControl.class.getSimpleName() + " could not find an updated implementation for this server version. " +
                    "However the plugin is trying to use the latest implementation which should work if Minecraft did not change drastically (last tested version: " + defaultInternalsVersion + ").");
            internals = new InternalsProvider();
        }
    }

    /**
     *
     * @param internalsName the current NMS version used by the server
     * @return the internals version name for the given NMS version. Returns defaultInternalsVersion for newer versions by default.
     */
    private static String getInternalsName(String internalsName) {
        Map<String, String> internalsVersions = new HashMap<>();
        Set<String> legacyVersionPack = Set.of("v1_13_R1", "v1_13_R2","v1_14_R1", "v1_15_R1", "v1_15_R2",
                "v1_16_R1", "v1_16_R2");
        for (String legacyVersion : legacyVersionPack) {
            internalsVersions.put(legacyVersion, "v1_16_R2");
        }
        internalsVersions.put("v1_16_R3", "v1_16_R3");
        return internalsVersions.getOrDefault(internalsName, defaultInternalsVersion);
    }

    static {
        ConfigurationSerialization.registerClass(TimeWorld.class, "TimeWorld");
    }

    private static TimeControl instance;

    @NotNull
    public static TimeControl getInstance() {
        return instance;
    }

    private WorldContainer worldContainer;

    @Override
    public void onEnable() {
        new Metrics(this, 3195);
        instance = this;
        this.getLogger().info("This plugin was made by alex_qp.");
        this.updateChecker();
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
        if (worldContainer != null)
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
        SleepManager sleepManager = SleepManager.build(this, internals, worldContainer);

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

    private void updateChecker() {
        int spigotResourceID = 70363;
        ConfigChecker configChecker = new ConfigChecker(this);
        ConfigurationSection updateCheckerSection = configChecker.checkConfigSection(this.getConfig(), "updatechecker", ConsoleErrorType.ERROR);
        if (updateCheckerSection != null && configChecker.checkBoolean(updateCheckerSection, "enable", ConsoleErrorType.WARN, true)) {
            ConsoleMessage.debug((Debugable) this, "enabled UpdateChecker");

            new UpdateChecker(this, UpdateCheckSource.SPIGOT, String.valueOf(spigotResourceID))
                    .setDownloadLink(spigotResourceID)
                    .setChangelogLink("https://www.spigotmc.org/resources/" + spigotResourceID + "/updates")
                    .setDonationLink("https://paypal.me/alexqpplugins")
                    .setNotifyOpsOnJoin(configChecker.checkBoolean(updateCheckerSection, "notify_op_on_login", ConsoleErrorType.WARN, true))
                    .setNotifyByPermissionOnJoin("phantomspawncontrol.updatechecker")
                    .checkEveryXHours(24).checkNow();
        }
    }
}

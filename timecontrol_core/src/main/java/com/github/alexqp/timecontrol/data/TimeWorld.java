package com.github.alexqp.timecontrol.data;

import com.google.common.collect.Range;
import com.github.alexqp.commons.config.ConfigChecker;
import com.github.alexqp.commons.config.ConfigurationSerializableCheckable;
import com.github.alexqp.commons.config.ConsoleErrorType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TimeWorld extends ConfigurationSerializableCheckable {

    private static final String[] configNames = {"minecraft-day-start-tick", "minecraft-day-length", "day-length", "night-length", "needed-sleep-percentage", "sleeping-time-multiplier", "storm-time-subtrahend"};

    public static String[] getConfigNamesCopy() {
        return configNames.clone();
    }

    private long dayStartTick = 0;
    private long defaultDayLength = 12000;

    private long dayLength = 24000;
    private long nightLength = 7000;
    private double allowSleep = 0.5;
    private int sleepTimeMultiplier = 20;
    private int stormTimeSubtrahend = 500;
    private Set<UUID> sleepingPlayers = new HashSet<>();

    private boolean isChanged = false;

    private static TimeWorld defTWorld = new TimeWorld();
    public static void setDefTimeWorld(@NotNull TimeWorld tWorld) {
        defTWorld = new TimeWorld(Objects.requireNonNull(tWorld));
    }

    public TimeWorld() {}

    public TimeWorld(long dayStartTick, long defaultDayLength, long dayLength, long nightLength, double allowSleepPercentage, int sleepTimeMultiplier, int stormTimeSubtrahend) {
        this.dayStartTick = dayStartTick;
        this.defaultDayLength = defaultDayLength;

        this.dayLength = dayLength;
        this.nightLength = nightLength;
        this.allowSleep = allowSleepPercentage;
        this.sleepTimeMultiplier = sleepTimeMultiplier;
        this.stormTimeSubtrahend = stormTimeSubtrahend;
    }

    public TimeWorld(TimeWorld tWorld) {
        this(tWorld.dayStartTick, tWorld.defaultDayLength,
                tWorld.dayLength, tWorld.nightLength, tWorld.allowSleep, tWorld.sleepTimeMultiplier, tWorld.stormTimeSubtrahend);
        this.sleepingPlayers = new HashSet<>();
        this.isChanged = true;
    }

    public boolean isChanged() {
        return this.isChanged;
    }
    public long getDayStartTick() {
        return dayStartTick;
    }
    public long getDefaultDayLength() {
        return defaultDayLength;
    }
    public long getDayLength() {
        return dayLength;
    }
    public long getNightLength() {
        return nightLength;
    }
    public int getSleepTimeMultiplier() {
        return sleepTimeMultiplier;
    }
    public int getStormTimeSubtrahend() {
        return stormTimeSubtrahend;
    }

    private boolean checkDefaultDayLength(long value) {
        return dayStartTick + value <= 24000;
    }
    private boolean checkDayStartTick(long value) {
        return Range.closed((long) 0, 24000 - defaultDayLength).contains(value);
    }
    private boolean checkLength(long value) {
        return value > 0;
    }
    private boolean checkAllowSleep(double value) {
        Range<Double> dRange = Range.closed(0.0, 1.0);
        return dRange.contains(value);
    }
    private boolean checkSleepTimeMultiplier(int value) {
        Range<Integer> range = Range.atLeast(0);
        return range.contains(value);
    }
    private boolean checkStormTimeSubtrahend(int value) {
        return Range.atLeast(-1).contains(value);
    }

    public void setChanged(boolean changed) {
        isChanged = changed;
    }
    public boolean setDayStartTick(long dayStartTick) {
        if (this.checkDayStartTick(dayStartTick)) {
            this.dayStartTick = dayStartTick;
            isChanged = true;
            return true;
        }
        return false;
    }
    public boolean setDefaultDayLength(long defaultDayLength) {
        if (this.checkDefaultDayLength(defaultDayLength)) {
            this.defaultDayLength = defaultDayLength;
            isChanged = true;
            return true;
        }
        return false;
    }
    public boolean setDayLength(long dayLength) {
        if (this.checkLength(dayLength)) {
            this.dayLength = dayLength;
            isChanged = true;
            return true;
        }
        return false;
    }
    public boolean setNightLength(long nightLength) {
        if (this.checkLength(nightLength)) {
            this.nightLength = nightLength;
            isChanged = true;
            return true;
        }
        return false;
    }
    public boolean setAllowSleep(double allowSleep) {
        if (this.checkAllowSleep(allowSleep)) {
            this.allowSleep = allowSleep;
            isChanged = true;
            return true;
        }
        return false;
    }
    public boolean setSleepTimeMultiplier(int sleepTimeMultiplier) {
        if (this.checkSleepTimeMultiplier(sleepTimeMultiplier)) {
            this.sleepTimeMultiplier = sleepTimeMultiplier;
            isChanged = true;
            return true;
        }
        return false;
    }
    public boolean setStormTimeSubtrahend(int stormTimeSubtrahend) {
        if (this.checkStormTimeSubtrahend(stormTimeSubtrahend)) {
            this.stormTimeSubtrahend = stormTimeSubtrahend;
            isChanged = true;
            return true;
        }
        return false;
    }

    public void addSleepingPlayer(Player p) {
        if (!sleepingPlayers.contains(p.getUniqueId()) && allowSleep > 0) {
            sleepingPlayers.add(p.getUniqueId());
        }
    }
    public void removeSleepingPlayer(Player p) {
        this.sleepingPlayers.remove(p.getUniqueId());
    }

    public int getAmountOfSleepingPlayers() {
        return sleepingPlayers.size();
    }

    public boolean hasEnoughSleepingPlayers(int countOfAllPlayersInWorld, List<Player> sleepingIgnoredPlayers) {
        int sleepingCount = this.getAmountOfSleepingPlayers();

        // is at least one person actual sleeping?
        if (sleepingCount <= 0)
            return false;

        // adding none sleeping sleeping-ignored players...
        for (Player p : sleepingIgnoredPlayers) {
            if (!sleepingPlayers.contains(p.getUniqueId()))
                sleepingCount++;
        }

        return countOfAllPlayersInWorld * allowSleep <= sleepingCount;
    }

    public boolean isDay(long time) {
        return (time <= (this.getDayStartTick() + this.getDefaultDayLength()));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put(configNames[0], this.getDayStartTick());
        map.put(configNames[1], this.getDefaultDayLength());

        map.put(configNames[2], this.getDayLength());
        map.put(configNames[3], this.getNightLength());
        map.put(configNames[4], this.allowSleep);
        map.put(configNames[5], this.getSleepTimeMultiplier());
        map.put(configNames[6], this.getStormTimeSubtrahend());
        return map;
    }

    @SuppressWarnings({"unused"})
    public static TimeWorld deserialize(Map<String, Object> map) {
        return new TimeWorld(
                (int) map.getOrDefault(configNames[0], defTWorld.dayStartTick),
                (int) map.getOrDefault(configNames[1], defTWorld.defaultDayLength),
                (int) map.getOrDefault(configNames[2], defTWorld.dayLength),
                (int) map.getOrDefault(configNames[3], defTWorld.nightLength),
                (double) map.getOrDefault(configNames[4], defTWorld.allowSleep),
                (int) map.getOrDefault(configNames[5], defTWorld.sleepTimeMultiplier),
                (int) map.getOrDefault(configNames[6], defTWorld.stormTimeSubtrahend));
    }

    @Override
    public boolean checkValues(ConfigChecker checker, ConfigurationSection section, String path, ConsoleErrorType errorType, boolean overwriteValues) {
        boolean isCorrect = true;

        String sectionPath = section.getCurrentPath() + "." + path;

        Range<Long> range = Range.closedOpen((long) 0, (long) 24000);
        if (!range.contains(defaultDayLength)) {
            checker.attemptConsoleMsg(errorType, sectionPath, configNames[1], ConfigChecker.getRangeMsg(range));
            isCorrect = false;
        }

        range = Range.closed((long) 0, 24000 - defaultDayLength);
        if (!range.contains(dayStartTick)) {
            dayStartTick = defTWorld.dayStartTick;
            checker.attemptConsoleMsg(errorType, sectionPath, configNames[0], dayStartTick, ConfigChecker.getRangeMsg(range));
            isCorrect = false;
        }

        range = Range.atLeast((long) 0);
        if (dayLength <= 0) {
            dayLength = defTWorld.dayLength;
            checker.attemptConsoleMsg(errorType, sectionPath, configNames[2], dayLength, ConfigChecker.getRangeMsg(range));
            isCorrect = false;
        }

        if (nightLength <= 0) {
            nightLength = defTWorld.nightLength;
            checker.attemptConsoleMsg(errorType, sectionPath, configNames[3], nightLength, ConfigChecker.getRangeMsg(range));
            isCorrect = false;
        }

        Range<Double> dRange = Range.closed((double) 0, (double) 1);
        if (!dRange.contains(allowSleep)) {
            allowSleep = defTWorld.allowSleep;
            checker.attemptConsoleMsg(errorType, sectionPath, configNames[4], allowSleep, ConfigChecker.getRangeMsg(dRange));
            isCorrect = false;
        }

        Range<Integer> iRange = Range.atLeast(0);
        if (!iRange.contains(sleepTimeMultiplier)) {
            sleepTimeMultiplier = defTWorld.sleepTimeMultiplier;
            checker.attemptConsoleMsg(errorType, sectionPath, configNames[5], sleepTimeMultiplier, ConfigChecker.getRangeMsg(iRange));
            isCorrect = false;
        }

        iRange = Range.atLeast(-1);
        if (!iRange.contains(stormTimeSubtrahend)) {
            stormTimeSubtrahend = defTWorld.stormTimeSubtrahend;
            checker.attemptConsoleMsg(errorType, sectionPath, configNames[6], stormTimeSubtrahend, ConfigChecker.getRangeMsg(iRange));
            isCorrect = false;
        }

        if (allowSleep == 1 && (sleepTimeMultiplier != 0 || stormTimeSubtrahend != -1)) {
            checker.attemptConsoleMsg(errorType, sectionPath, configNames[4] + " & " + configNames[5], "Please be aware that " + configNames[4] + " set to 1 / " + configNames[6] + " set to -1 will always skip the night/the storm instantly.");
        }

        if (!isCorrect) {
            isChanged = true;
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MDS = " + this.dayStartTick
                + ", MDL = " + this.defaultDayLength
                + ", DL = " + this.dayLength
                + ", NL = " + this.nightLength
                + ", NSP = " + this.allowSleep
                + ", STM = " + this.sleepTimeMultiplier
                + ", STS = " + this.stormTimeSubtrahend
                + ", CS = " + this.getAmountOfSleepingPlayers();
    }

    public static String getPrefixes() {
        return "MDS = " + configNames[0]
                + ", MDL = " + configNames[1]
                + ", DL = " + configNames[2]
                + ", NL = " + configNames[3]
                + ", NSP = " + configNames[4]
                + ", STM = " + configNames[5]
                + ", STS = " + configNames[6]
                + ", CS = current sleeping players";
    }
}

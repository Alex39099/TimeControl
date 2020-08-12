package com.github.alexqp.timecontrol.data;

import com.github.alexqp.commons.messages.ConsoleMessage;
import com.google.common.collect.Range;
import com.github.alexqp.commons.config.ConfigChecker;
import com.github.alexqp.commons.config.ConfigurationSerializableCheckable;
import com.github.alexqp.commons.config.ConsoleErrorType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TimeWorld implements ConfigurationSerializableCheckable {


    public static class Stat<T extends Comparable<T>> {

        public static final Stat<Integer> DEFAULT_DAY_LENGTH = new Stat<>("mcdaylength", "minecraft-day-length", 12000, Range.closed(0, 24000));
        public static final Stat<Integer> DAY_START_TICK = new Stat<>("mcdaystart", "minecraft-day-start-tick", 0, Range.closed(0, 24000 - DEFAULT_DAY_LENGTH.getDefValue()));
        public static final Stat<Integer> DAY_LENGTH = new Stat<>("daylength", "day-length", 24000, Range.atLeast(1));
        public static final Stat<Integer> NIGHT_LENGTH = new Stat<>("nightlength", "night-length", 7000, Range.atLeast(1));
        public static final Stat<Double> NEEDED_SLEEP_PERCENTAGE = new Stat<>("neededsleeppercentage", "needed-sleep-percentage", 0.5, Range.closed(0.0, 1.0));
        public static final Stat<Integer> NIGHT_TIME_MULTIPLIER = new Stat<>("sleepingnighttimemultiplier", "sleeping-night-time-multiplier", 20, Range.atLeast(-1));
        public static final Stat<Integer> STORM_TIME_MULTIPLIER = new Stat<>("sleepingstormtimemultiplier", "sleeping-storm-time-multiplier", 60, Range.atLeast(-1));
        // wanna add sth? -> Do not forget getMethod, setMethod, setValueByStat, getPrefixes, toString!

        private static final List<Stat<?>> values = new ArrayList<Stat<?>>() {{
            add(DEFAULT_DAY_LENGTH);
            add(DAY_START_TICK);
            add(DAY_LENGTH);
            add(NIGHT_LENGTH);
            add(NEEDED_SLEEP_PERCENTAGE);
            add(NIGHT_TIME_MULTIPLIER);
            add(STORM_TIME_MULTIPLIER);
            this.sort(Comparator.comparing(Stat::getName));
        }};

        @NotNull
        public static List<Stat<?>> values() {
            return values;
        }

        private final String name;
        private final String configName;
        private T defValue;
        private final Range<T> range;

        private Stat(@NotNull String name, @NotNull String configName, @NotNull T defValue, @NotNull Range<T> range) {
            this.name = name;
            this.configName = configName;
            this.range = range;
            this.defValue = defValue;
        }

        @NotNull
        public String getName() {
            return this.name;
        }

        @NotNull
        public String getConfigName() {
            return this.configName;
        }

        @NotNull
        public T getDefValue() {
            return this.defValue;
        }

        public void setDefValue(T defValue) throws IllegalArgumentException {
            if (!range.contains(defValue))
                throw new IllegalArgumentException("defValue is not within bounds of range.");
            this.defValue = defValue;
        }

        @NotNull
        public Range<T> getAllowedRange() {
            return this.range;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Stat) {
                return this.getName().equals(((Stat<?>) obj).getName());
            }
            return false;
        }
    }

    private int dayStartTick = Stat.DAY_START_TICK.getDefValue();
    private int defaultDayLength = Stat.DEFAULT_DAY_LENGTH.getDefValue();

    private int dayLength = Stat.DAY_LENGTH.getDefValue();
    private int nightLength = Stat.NIGHT_LENGTH.getDefValue();

    private double neededSleepPercentage = Stat.NEEDED_SLEEP_PERCENTAGE.getDefValue();
    private int nightTimeMultiplier = Stat.NIGHT_TIME_MULTIPLIER.getDefValue();
    private int stormTimeMultiplier = Stat.STORM_TIME_MULTIPLIER.getDefValue();

    private boolean isChanged = false;

    public static void setDefValues(@NotNull TimeWorld tWorld) {
        Stat.DEFAULT_DAY_LENGTH.setDefValue(tWorld.getDefaultDayLength());
        Stat.DAY_START_TICK.setDefValue(tWorld.getDayStartTick());
        Stat.DAY_LENGTH.setDefValue(tWorld.getDayLength());
        Stat.NIGHT_LENGTH.setDefValue(tWorld.getNightLength());
        Stat.NEEDED_SLEEP_PERCENTAGE.setDefValue(tWorld.getNeededSleepPercentage());
        Stat.NIGHT_TIME_MULTIPLIER.setDefValue(tWorld.getNightTimeMultiplier());
        Stat.STORM_TIME_MULTIPLIER.setDefValue(tWorld.getStormTimeMultiplier());
    }

    public TimeWorld() {}

    public TimeWorld(int dayStartTick, int defaultDayLength, int dayLength, int nightLength, double neededSleepPercentage, int nightTimeMultiplier, int stormTimeMultiplier) {
        this.dayStartTick = dayStartTick;
        this.defaultDayLength = defaultDayLength;
        this.dayLength = dayLength;
        this.nightLength = nightLength;
        this.neededSleepPercentage = neededSleepPercentage;
        this.nightTimeMultiplier = nightTimeMultiplier;
        this.stormTimeMultiplier = stormTimeMultiplier;
    }

    public boolean isChanged() {
        return this.isChanged;
    }
    public int getDayStartTick() {
        return dayStartTick;
    }
    public int getDefaultDayLength() {
        return defaultDayLength;
    }
    public int getDayLength() {
        return dayLength;
    }
    public int getNightLength() {
        return nightLength;
    }
    public double getNeededSleepPercentage() {
        return this.neededSleepPercentage;
    }
    public int getNightTimeMultiplier() {
        return nightTimeMultiplier;
    }
    public int getStormTimeMultiplier() {
        return stormTimeMultiplier;
    }

    public void setChanged(boolean changed) {
        isChanged = changed;
    }
    public boolean setDayStartTick(int dayStartTick) {
        if (Stat.DAY_START_TICK.getAllowedRange().contains(dayStartTick)) {
            this.dayStartTick = dayStartTick;
            isChanged = true;
            return true;
        }
        return false;
    }
    public boolean setDefaultDayLength(int defaultDayLength) {
        if (Stat.DEFAULT_DAY_LENGTH.getAllowedRange().contains(defaultDayLength)) {
            this.defaultDayLength = defaultDayLength;
            isChanged = true;
            return true;
        }
        return false;
    }
    public boolean setDayLength(int dayLength) {
        if (Stat.DAY_LENGTH.getAllowedRange().contains(dayLength)) {
            this.dayLength = dayLength;
            isChanged = true;
            return true;
        }
        return false;
    }
    public boolean setNightLength(int nightLength) {
        if (Stat.NIGHT_LENGTH.getAllowedRange().contains(nightLength)) {
            this.nightLength = nightLength;
            isChanged = true;
            return true;
        }
        return false;
    }
    public boolean setNeededSleepPercentage(double allowSleep) {
        if (Stat.NEEDED_SLEEP_PERCENTAGE.getAllowedRange().contains(neededSleepPercentage)) {
            this.neededSleepPercentage = allowSleep;
            isChanged = true;
            return true;
        }
        return false;
    }
    public boolean setNightTimeMultiplier(int multiplier) {
        if (Stat.NIGHT_TIME_MULTIPLIER.getAllowedRange().contains(multiplier)) {
            this.nightTimeMultiplier = multiplier;
            isChanged = true;
            return true;
        }
        return false;
    }
    public boolean setStormTimeMultiplier(int multiplier) {
        if (Stat.STORM_TIME_MULTIPLIER.getAllowedRange().contains(multiplier)) {
            this.stormTimeMultiplier = multiplier;
            isChanged = true;
            return true;
        }
        return false;
    }

    public boolean isDay(long time) {
        return (time <= (this.getDayStartTick() + this.getDefaultDayLength()));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put(Stat.DAY_START_TICK.getConfigName(), this.getDayStartTick());
        map.put(Stat.DEFAULT_DAY_LENGTH.getConfigName(), this.getDefaultDayLength());
        map.put(Stat.DAY_LENGTH.getConfigName(), this.getDayLength());
        map.put(Stat.NIGHT_LENGTH.getConfigName(), this.getNightLength());
        map.put(Stat.NEEDED_SLEEP_PERCENTAGE.getConfigName(), this.getNeededSleepPercentage());
        map.put(Stat.NIGHT_TIME_MULTIPLIER.getConfigName(), this.getNightTimeMultiplier());
        map.put(Stat.STORM_TIME_MULTIPLIER.getConfigName(), this.getStormTimeMultiplier());
        return map;
    }

    @SuppressWarnings({"unused"})
    public static TimeWorld deserialize(Map<String, Object> map) {
        TimeWorld defTimeWorld = new TimeWorld();
        return new TimeWorld(
                (int) map.getOrDefault(Stat.DAY_START_TICK.getConfigName(), defTimeWorld.dayStartTick),
                (int) map.getOrDefault(Stat.DEFAULT_DAY_LENGTH.getConfigName(), defTimeWorld.defaultDayLength),
                (int) map.getOrDefault(Stat.DAY_LENGTH.getConfigName(), defTimeWorld.dayLength),
                (int) map.getOrDefault(Stat.NIGHT_LENGTH.getConfigName(), defTimeWorld.nightLength),
                (double) map.getOrDefault(Stat.NEEDED_SLEEP_PERCENTAGE.getConfigName(), defTimeWorld.neededSleepPercentage),
                (int) map.getOrDefault(Stat.NIGHT_TIME_MULTIPLIER.getConfigName(), defTimeWorld.nightTimeMultiplier),
                (int) map.getOrDefault(Stat.STORM_TIME_MULTIPLIER.getConfigName(), defTimeWorld.stormTimeMultiplier));
    }

    public <T extends Comparable<T>> boolean setValueByStat(Stat<T> stat, T value) {
        if (stat.equals(Stat.DAY_START_TICK))
            return this.setDayStartTick((Integer) value);
        else if (stat.equals(Stat.DEFAULT_DAY_LENGTH))
            return this.setDefaultDayLength((Integer) value);
        else if (stat.equals(Stat.DAY_LENGTH))
            return this.setDayLength((Integer) value);
        else if (stat.equals(Stat.NIGHT_LENGTH))
            return this.setNightLength((Integer) value);
        else if (stat.equals(Stat.NEEDED_SLEEP_PERCENTAGE))
            return this.setNeededSleepPercentage((Double) value);
        else if (stat.equals(Stat.NIGHT_TIME_MULTIPLIER))
            return this.setNightTimeMultiplier((Integer) value);
        else if (stat.equals(Stat.STORM_TIME_MULTIPLIER))
            return this.setStormTimeMultiplier((Integer) value);
        return false;
    }

    private <T extends Comparable<T>> boolean checkValue(Stat<T> stat, T value, ConfigChecker checker, String sectionPath, ConsoleErrorType errorType, boolean overwriteValue) {
        if (!stat.getAllowedRange().contains(value)) {
            checker.attemptConsoleMsg(errorType, sectionPath, stat.getConfigName(), ConfigChecker.getRangeMsg(stat.getAllowedRange()));
            if (overwriteValue)
                this.setValueByStat(stat, value);
            return false;
        }
        return true;
    }

    @Override
    public boolean checkValues(ConfigChecker checker, ConfigurationSection section, String path, ConsoleErrorType errorType, boolean overwriteValues) {
        String sectionPath = section.getCurrentPath() + "." + path;
        boolean isCorrect = true;

        if (!checkValue(Stat.DAY_START_TICK, this.dayStartTick, checker, sectionPath, errorType, overwriteValues))
            isCorrect = false;
        if (!checkValue(Stat.DEFAULT_DAY_LENGTH, this.defaultDayLength, checker, sectionPath, errorType, overwriteValues))
            isCorrect = false;
        if (!checkValue(Stat.DAY_LENGTH, this.dayLength, checker, sectionPath, errorType, overwriteValues))
            isCorrect = false;
        if (!checkValue(Stat.NIGHT_LENGTH, this.nightLength, checker, sectionPath, errorType, overwriteValues))
            isCorrect = false;
        if (!checkValue(Stat.NEEDED_SLEEP_PERCENTAGE, this.neededSleepPercentage, checker, sectionPath, errorType, overwriteValues))
            isCorrect = false;
        if (!checkValue(Stat.NIGHT_TIME_MULTIPLIER, this.nightTimeMultiplier, checker, sectionPath, errorType, overwriteValues))
            isCorrect = false;
        if (!checkValue(Stat.STORM_TIME_MULTIPLIER, this.stormTimeMultiplier, checker, sectionPath, errorType, overwriteValues))
            isCorrect = false;

        return isCorrect;
    }

    @Override
    public String toString() {
        return "MDS = " + this.dayStartTick
                + ", MDL = " + this.defaultDayLength
                + ", DL = " + this.dayLength
                + ", NL = " + this.nightLength
                + ", NSP = " + this.neededSleepPercentage
                + ", STM = " + this.nightTimeMultiplier
                + ", STS = " + this.stormTimeMultiplier;
    }

    public static String getPrefixes() {
        return "MDS = " + Stat.DAY_START_TICK.getConfigName()
                + ", MDL = " + Stat.DEFAULT_DAY_LENGTH.getConfigName()
                + ", DL = " + Stat.DAY_LENGTH.getConfigName()
                + ", NL = " + Stat.NIGHT_LENGTH.getConfigName()
                + ", NSP = " + Stat.NEEDED_SLEEP_PERCENTAGE.getConfigName()
                + ", NTM = " + Stat.NIGHT_TIME_MULTIPLIER.getConfigName()
                + ", STM = " + Stat.STORM_TIME_MULTIPLIER.getConfigName();
    }
}

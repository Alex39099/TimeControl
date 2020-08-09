package com.github.alexqp.timecontrol.command;

import com.github.alexqp.commons.command.AlexCommand;
import com.github.alexqp.commons.command.AlexSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import com.github.alexqp.timecontrol.data.TimeWorld;
import com.github.alexqp.timecontrol.data.WorldContainer;

import java.util.ArrayList;
import java.util.List;

public class SubCommandSet extends AlexSubCommand {

    private static final String name = "set";
    private static final String permission = "timecontrol.edit";
    private static final String permissionPrefix = "timecontrol.change.";

    private static final String[] timeWorldOptions = TimeWorld.getConfigNamesCopy();
    private enum SetCommandOptions {
        // do not forget to also add in execute!!!

        DAY_START_TICK(0, "mcdaystart"),
        DEFAULT_DAY_LENGTH(1, "mcdaylength"),
        DAY_LENGTH(2, "daylength"),
        NIGHT_LENGTH(3, "nightlength"),
        NEEDED_SLEEP_PERCENTAGE(4, "sleeppercentage"),
        SLEEPING_TIME_MULTIPLIER(5, "sleeptimemultiplier"),
        STORM_TIME_SUBTRAHEND(6, "stormtimesubtrahend");

        private String configName;
        private String permission;

        SetCommandOptions(int optionsIndex, String permission) {
            this.configName = timeWorldOptions[optionsIndex];
            this.permission = permission;
        }

        public String getConfigName() {
            return configName;
        }
        public String getPermission() {
            return permissionPrefix + permission;
        }

        public static SetCommandOptions getOptionByConfigName(String optionName) {
            for (SetCommandOptions option : SetCommandOptions.values()) {
                if (option.getConfigName().equalsIgnoreCase(optionName)) {
                    return option;
                }
            }
            return null;
        }
    }

    private WorldContainer worldContainer;
    private String successMsg;
    private String invalidValueMsg;

    SubCommandSet(String helpLine, AlexCommand alexCommand, WorldContainer container, String successMsg, String invalidValueMsg) {
        super(name, helpLine, alexCommand);
        this.setPermission(permission);
        this.setCmdParamLine("<option> <value> [world]");
        this.setUsageLine(alexCommand.getUsagePrefixDummy() + "/" + alexCommand.getName() + " " + this.getName() + this.getCmdParamLine() + ". Options: " + ArrayToString(timeWorldOptions));
        this.worldContainer = container;
        this.successMsg = successMsg;
        this.invalidValueMsg = invalidValueMsg;
    }

    private static String ArrayToString(String[] array) {
        StringBuilder arrayString = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            arrayString.append(array[i]);
            if (i + 1 < array.length)
                arrayString.append(", ");
        }
        return arrayString.toString();
    }

    private boolean hasPermission(@NotNull CommandSender sender, String optionPermission) {
        return sender.hasPermission(optionPermission) || sender.hasPermission(permissionPrefix + "*");
    }

    @Override
    protected boolean execute(CommandSender sender, String label, String extraArgument, @NotNull String[] args) {

        if (args.length > 3 || args.length < 2)
            return false;

        if (args.length != 3 && (sender instanceof ConsoleCommandSender)) {
            sendPrefixColorMessage(sender, "&cvalue must be specified if command is performed in console.");
            return true;
        }


        World world;
        if (args.length == 3)
            world = Bukkit.getWorld(args[2]);
        else
            world = ((Player) sender).getWorld();

        if (world == null) {
            return false;
        }

        TimeWorld tWorld = worldContainer.getTimeWorldForWorld(world);
        SetCommandOptions option = SetCommandOptions.getOptionByConfigName(args[0]);
        if (option == null)
            return false;

        if (this.hasPermission(sender, option.getPermission())) {

            int value = Integer.parseInt(args[1]);
            boolean isCorrect = false;

            if (option.equals(SetCommandOptions.DAY_START_TICK)) {
                isCorrect = tWorld.setDayStartTick(value);
            } else if (option.equals(SetCommandOptions.DEFAULT_DAY_LENGTH)) {
                isCorrect = tWorld.setDefaultDayLength(value);
            } else if (option.equals(SetCommandOptions.DAY_LENGTH)) {
                isCorrect = tWorld.setDayLength(value);
            } else if (option.equals(SetCommandOptions.NIGHT_LENGTH)) {
                isCorrect = tWorld.setNightLength(value);
            } else if (option.equals(SetCommandOptions.NEEDED_SLEEP_PERCENTAGE)) {
                double dValue = Double.parseDouble(args[1]);
                isCorrect = tWorld.setAllowSleep(dValue);
            } else if (option.equals(SetCommandOptions.SLEEPING_TIME_MULTIPLIER)) {
                isCorrect = tWorld.setSleepTimeMultiplier(value);
            } else if (option.equals(SetCommandOptions.STORM_TIME_SUBTRAHEND)) {
                isCorrect = tWorld.setStormTimeSubtrahend(value);
            }

            if (isCorrect) {
                sendPrefixColorMessage(sender, successMsg);
                return true;
            } else {
                sendPrefixColorMessage(sender, invalidValueMsg);
                return false;
            }
        } else {
            sendPrefixColorMessage(sender, this.getNoPermissionLine());
            return true;
        }
    }

    @Override
    protected List<String> getTabCompletion(CommandSender sender, String extraArgument, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (SetCommandOptions option : SetCommandOptions.values()) {
                if (hasPermission(sender, option.getPermission())) {
                    list.add(option.getConfigName());
                }
            }
            StringUtil.copyPartialMatches(args[0], list, completions);
        }
        return completions;
    }
}

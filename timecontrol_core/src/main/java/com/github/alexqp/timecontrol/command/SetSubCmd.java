package com.github.alexqp.timecontrol.command;

import com.github.alexqp.commons.command.AlexSubCommand;
import com.github.alexqp.timecontrol.data.TimeWorld;
import com.github.alexqp.timecontrol.data.WorldContainer;
import com.google.common.collect.Range;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SetSubCmd extends AlexSubCommand {

    private WorldContainer worldContainer;

    private BaseComponent success;

    SetSubCmd(@NotNull AlexSubCommand parent, @NotNull WorldContainer worldContainer) {
        super("set", "sets the specified option.", parent);

        this.worldContainer = worldContainer;

        this.setCmdParamLine(new TextComponent("<option> <value> [world]"));

        this.success = this.getPrefixMessage(new ComponentBuilder("Value successfully changed.").color(ChatColor.DARK_GREEN).create());
        this.makeFinal();
    }

    private void sendInvalidTypeError(CommandSender sender, String value) {
        sendMessage(sender, this.getPrefixMessage(new ComponentBuilder(value + " is no valid number.").color(ChatColor.RED).create()));
    }

    private void sendInvalidValueError(CommandSender sender, Range<?> range) {
        sendMessage(sender, this.getPrefixMessage(new ComponentBuilder("Invalid value. Value must be within " + range.toString()).color(ChatColor.RED).create()));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label,
                           @NotNull List<AlexSubCommand> previousCmds, @NotNull List<String> previousExtraArguments,
                           @NotNull String[] args, int startIndex) {

        if (args.length > startIndex + 3 || args.length < startIndex + 2)
            return false;

        if (args.length != startIndex + 3 && (sender instanceof ConsoleCommandSender)) {
            sendMessage(sender, this.getPrefixMessage(new ComponentBuilder("world must be specified if command is performed in console").color(ChatColor.RED).create()));
            return false;
        }

        World world;
        if (args.length == 3 + startIndex)
            world = Bukkit.getWorld(args[startIndex + 2]);
        else
            world = ((Player) sender).getWorld();

        if (world == null) {
            sendMessage(sender, this.getPrefixMessage(new ComponentBuilder(args[startIndex + 2] + " is no valid world.").color(ChatColor.RED).create()));
            return false;
        }

        TimeWorld.Stat<? extends Comparable<?>> option = getOptionByConfigName(args[startIndex]);
        if (option == null) {
            sendMessage(sender, this.getPrefixMessage(new ComponentBuilder(args[startIndex] + " is no valid option.").color(ChatColor.RED).create()));
            return false;
        }

        TimeWorld tWorld = worldContainer.getTimeWorldForWorld(world);

        if (sender.hasPermission(this.getPermission() + "." + option.getName())) {
            try {
                @SuppressWarnings("unchecked")
                TimeWorld.Stat<Integer> integerStat = (TimeWorld.Stat<Integer>) option;
                if (tWorld.setValueByStat(integerStat, Integer.parseInt(args[startIndex + 1]))) {
                    sendMessage(sender, success);
                    return true;
                }

                this.sendInvalidValueError(sender, integerStat.getAllowedRange());
            }
            catch (NumberFormatException e) {
                sendInvalidTypeError(sender, args[startIndex + 1]);
            }
            catch (ClassCastException ignored) {}

            try {
                @SuppressWarnings("unchecked")
                TimeWorld.Stat<Double> doubleStat = (TimeWorld.Stat<Double>) option;
                if (tWorld.setValueByStat(doubleStat, Double.parseDouble(args[startIndex + 1]))) {
                    sendMessage(sender, success);
                    return true;
                }

                this.sendInvalidValueError(sender, doubleStat.getAllowedRange());
            }
            catch (NumberFormatException e) {
                sendInvalidTypeError(sender, args[startIndex + 1]);
            }
            catch (ClassCastException ignored) {}
        } else {
            sendMessage(sender, Objects.requireNonNull(this.getNoPermissionLine()));
        }

        return false;
    }

    @Nullable
    private TimeWorld.Stat<? extends Comparable<?>> getOptionByConfigName(String configName) {
        for (TimeWorld.Stat<?> option : TimeWorld.Stat.values()) {
            if (option.getConfigName().equalsIgnoreCase(configName)) {
                return option;
            }
        }
        return null;
    }

    @Override
    protected @NotNull List<String> additionalTabCompleterOptions(@NotNull CommandSender sender, @NotNull String label, @NotNull List<AlexSubCommand> previousCmds, @NotNull List<String> previousExtraArguments, @NotNull String[] args, int startIndex) {
        List<String> completions = new ArrayList<>();
        for (TimeWorld.Stat<?> option : TimeWorld.Stat.values()) {
            completions.add(option.getConfigName());
        }
        return completions;
    }

    @Override
    protected @NotNull List<String> getTabCompletion(@NotNull CommandSender sender, @NotNull String label, @NotNull List<AlexSubCommand> previousCmds, @NotNull List<String> previousExtraArguments, @NotNull String[] args, int startIndex) {
        List<String> completions = new ArrayList<>();
        if (args.length > startIndex + 1) {
            StringUtil.copyPartialMatches(args[startIndex + 1], new ArrayList<>(worldContainer.getConfigWorldNames()), completions);
        }
        Collections.sort(completions);
        return completions;
    }
}

package com.github.alexqp.timecontrol.command;

import com.github.alexqp.commons.command.AlexSubCommand;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;
import com.github.alexqp.timecontrol.data.WorldContainer;
import com.github.alexqp.timecontrol.main.InternalsProvider;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UninstallSubCmd extends AlexSubCommand {

    private JavaPlugin plugin;
    private InternalsProvider internals;
    private WorldContainer worldContainer;

    UninstallSubCmd(@NotNull AlexSubCommand parent, @NotNull JavaPlugin plugin, @NotNull InternalsProvider internals, @NotNull WorldContainer worldContainer) {
        super("uninstall", "Re-enables all gameRules and disables the plugin.", parent);
        this.setIsPlayerCmd(false);
        this.plugin = plugin;
        this.worldContainer = worldContainer;
        this.internals = internals;
        this.makeFinal();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label,
                           @NotNull List<AlexSubCommand> previousCmds, @NotNull List<String> previousExtraArguments,
                           @NotNull String[] args, int startIndex) {
        for (String worldName : worldContainer.getConfigWorldNames()) {
            World world = Bukkit.getServer().getWorld(worldName);
            if (world != null) {
                if (internals.setDayLightCycle(world, true)) {
                    plugin.getLogger().info("(re-)enabled doDayLightCycle for world " + worldName);
                } else {
                    ConsoleMessage.send(ConsoleErrorType.ERROR, plugin, "Could not re-enable doDayLightCycle for world " + worldName);
                }
            } else {
                ConsoleMessage.debug(this.getClass(), plugin, "could not find a valid world for configWorld " + worldName);
            }
        }
        plugin.getLogger().info("If you had problems with the plugin feel free to contact me (alex_qp) on spigot.");
        plugin.getLogger().info("If you were unhappy with the plugin because of bad performance or sth like that please consider leaving a review/suggestion to help me improve.");
        plugin.getLogger().info("Plugin will now disable, please delete the jar file before restarting/reloading your server");
        plugin.onDisable();
        return true;
    }
}

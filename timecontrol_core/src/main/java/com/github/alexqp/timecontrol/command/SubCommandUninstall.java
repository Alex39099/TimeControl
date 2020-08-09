package com.github.alexqp.timecontrol.command;

import com.github.alexqp.commons.command.AlexSubCommand;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import com.github.alexqp.timecontrol.data.WorldContainer;
import com.github.alexqp.timecontrol.main.InternalsProvider;

public class SubCommandUninstall extends AlexSubCommand {

    private static final String name = "uninstall";

    private JavaPlugin plugin;
    private InternalsProvider internals;
    private WorldContainer worldContainer;

    SubCommandUninstall(String helpLine, JavaPlugin plugin, InternalsProvider internals, WorldContainer container) {
        super(name, helpLine);
        this.setIsPlayerCmd(false);
        this.plugin = plugin;
        this.internals = internals;
        this.worldContainer = container;
    }

    @Override
    protected boolean execute(CommandSender commandSender, String s, String extraArgument, String[] strings) {
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
        plugin.getLogger().info("If you were unhappy with the plugin because of bad performance or sth like that please consider leaving a review/suggestions to help me improve.");
        plugin.getLogger().info("Plugin will no disable, please delete the jar file before restarting/reloading your server");
        plugin.onDisable();
        return true;
    }
}

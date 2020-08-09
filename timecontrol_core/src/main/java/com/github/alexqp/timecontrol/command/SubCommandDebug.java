package com.github.alexqp.timecontrol.command;

import com.github.alexqp.commons.command.AlexCommand;
import com.github.alexqp.commons.command.AlexSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import com.github.alexqp.timecontrol.data.TimeWorld;
import com.github.alexqp.timecontrol.data.WorldContainer;

import java.util.ArrayList;
import java.util.List;

public class SubCommandDebug extends AlexSubCommand {

    private static final String name = "debug";
    @SuppressWarnings("FieldCanBeLocal")
    private final String permission = "timecontrol.debug";
    private final String headingsPrefix = "" + ChatColor.GOLD;

    private WorldContainer worldContainer;
    private String debugHeader;

    SubCommandDebug(JavaPlugin plugin, String helpLine, AlexCommand alexCommand, WorldContainer container) {
        super(name, helpLine, alexCommand);
        this.setPermission(permission);
        this.worldContainer = container;
        this.debugHeader = ChatColor.DARK_PURPLE + "Debug messages: (" + plugin.getName() + " v" + plugin.getDescription().getVersion() + ")";
    }

    private String getEnabledWorlds() {
        List<String> worldNames = new ArrayList<>();
        for (World world : Bukkit.getServer().getWorlds()) {
            if (worldContainer.isEnabled(world)) {
                worldNames.add(world.getName());
            }
        }
        return worldNames.toString();
    }

    @Override
    protected boolean execute(CommandSender sender, String label, String extraArgument, String[] args) {
        sendPrefixColorMessage(sender, debugHeader);
        sendPrefixColorMessage(sender, headingsPrefix + "Current config and enabled worlds:");
        sendPrefixColorMessage(sender, "ConfigWorlds = " + worldContainer.getConfigWorldNames());
        sendPrefixColorMessage(sender, "EnabledWorlds = " + this.getEnabledWorlds());
        sendPrefixColorMessage(sender, "");
        sendPrefixColorMessage(sender, headingsPrefix + "Current World-Data");
        sendPrefixColorMessage(sender, ChatColor.YELLOW + "Prefixes = " + ChatColor.WHITE + TimeWorld.getPrefixes());
        sendPrefixColorMessage(sender, "");
        for (String worldName : worldContainer.getConfigWorldNames())
            sendPrefixColorMessage(sender, ChatColor.YELLOW + worldName + ": " + ChatColor.RESET + worldContainer.getTimeWorldForWorld(worldName).toString());

        return true;
    }
}

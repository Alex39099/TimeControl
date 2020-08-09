package com.github.alexqp.timecontrol.command;

import com.github.alexqp.commons.command.AlexCommand;
import com.github.alexqp.commons.command.AlexSubCommand;
import org.bukkit.command.CommandSender;
import com.github.alexqp.timecontrol.main.TimeControl;

public class SubCommandReload extends AlexSubCommand {

    private static final String name = "reload";
    @SuppressWarnings("FieldCanBeLocal")
    private final String permission = "timecontrol.reload";

    private TimeControl plugin;

    SubCommandReload(TimeControl plugin, String helpLine, AlexCommand alexCommand) {
        super(name, helpLine, alexCommand);
        this.setPermission(permission);
        this.plugin = plugin;
    }

    @Override
    protected boolean execute(CommandSender sender, String label, String extraArgument, String[] strings) {
        plugin.onReload();
        sendPrefixColorMessage(sender, "Plugin reloaded!");
        return true;
    }
}

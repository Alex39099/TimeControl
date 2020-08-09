package com.github.alexqp.timecontrol.command;

import com.github.alexqp.commons.command.AlexCommand;
import com.github.alexqp.commons.config.ConfigChecker;
import com.github.alexqp.commons.config.ConsoleErrorType;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import com.github.alexqp.timecontrol.data.WorldContainer;
import com.github.alexqp.timecontrol.main.InternalsProvider;
import com.github.alexqp.timecontrol.main.TimeControl;

@SuppressWarnings("FieldCanBeLocal")
public class TimeControlCommand extends AlexCommand {

    private static final String name = "timecontrol";

    private final String messagesSectionConfigName = "messages";
    private final String creditsConfigName = "credits";
    private final String[] helpSectionConfigNames = {"help", "header"};
    private final String noPermissionConfigName = "noPerm";
    private final String[] wrongCmdUsageConfigNames = {"wrongCmdUsage", "prefix", "noSubCmd"};
    private final String[] setCmdConfigNames = {"setCmd", "success", "invalidValue"};

    public TimeControlCommand(TimeControl plugin, InternalsProvider internals, WorldContainer worldContainer) {
        super(name, plugin, ChatColor.AQUA);
        ConfigChecker configChecker = new ConfigChecker(plugin);

        ConfigurationSection msgSection = configChecker.checkConfigSection(plugin.getConfig(), messagesSectionConfigName, ConsoleErrorType.ERROR);
        if (msgSection == null)
            return;

        this.addCreditLine(configChecker.checkString(msgSection, creditsConfigName, ConsoleErrorType.WARN, "Use /timecontrol help for all available commands"));
        this.setNoPermissionLine(configChecker.checkString(msgSection, noPermissionConfigName, ConsoleErrorType.WARN, "&4You do not have permission."));


        String reloadHelpLine = "Reloads the plugin";
        String setHelpLine = "Sets the specified option.";
        String debugHelpLine = "Shows some debug messages.";
        String uninstallHelpLine = "Re-enables all gamerules and disables the plugin.";

        ConfigurationSection section = configChecker.checkConfigSection(msgSection, helpSectionConfigNames[0], ConsoleErrorType.ERROR);
        if (section != null) {
            this.addHelpCmdHeaderLine(configChecker.checkString(section, helpSectionConfigNames[1], ConsoleErrorType.WARN, "List of all available commands:"));

            reloadHelpLine = configChecker.checkString(section, "reload", ConsoleErrorType.WARN, reloadHelpLine);
            setHelpLine = configChecker.checkString(section, "set", ConsoleErrorType.WARN, setHelpLine);
            debugHelpLine = configChecker.checkString(section, "debug", ConsoleErrorType.WARN, debugHelpLine);
            uninstallHelpLine = configChecker.checkString(section, "uninstall", ConsoleErrorType.WARN, uninstallHelpLine);
        }

        section = configChecker.checkConfigSection(msgSection, wrongCmdUsageConfigNames[0], ConsoleErrorType.ERROR);
        if (section != null) {
            this.setUsagePrefixDummy(configChecker.checkString(section, wrongCmdUsageConfigNames[1], ConsoleErrorType.WARN, "&CUsage:"));
            this.setUsageLine(configChecker.checkString(section, wrongCmdUsageConfigNames[2], ConsoleErrorType.WARN, "&CNo such (sub-)command. Use /com.github.alexqp.timecontrol help for all available commands."));
        }

        String setCmdSuccessMsg = "&2value successfully changed.";
        String setCmdInvalidValueMsg = "&4value is not allowed for that option.";
        section = configChecker.checkConfigSection(msgSection, setCmdConfigNames[0], ConsoleErrorType.ERROR);
        if (section != null) {
            setCmdSuccessMsg = configChecker.checkString(section, setCmdConfigNames[1], ConsoleErrorType.WARN, setCmdSuccessMsg);
            setCmdInvalidValueMsg = configChecker.checkString(section, setCmdConfigNames[2], ConsoleErrorType.WARN, setCmdInvalidValueMsg);
        }

        SubCommandReload reloadSubCommand = new SubCommandReload(plugin, reloadHelpLine, this);
        SubCommandDebug debugSubCommand = new SubCommandDebug(plugin, debugHelpLine, this, worldContainer);
        SubCommandSet setSubCommand = new SubCommandSet(setHelpLine, this, worldContainer, setCmdSuccessMsg, setCmdInvalidValueMsg);
        SubCommandUninstall uninstallSubCommand = new SubCommandUninstall(uninstallHelpLine, plugin, internals, worldContainer);

        this.addSubCommand(reloadSubCommand);
        this.addSubCommand(debugSubCommand);
        this.addSubCommand(setSubCommand);
        this.addSubCommand(uninstallSubCommand);
    }
}

/*
 * Copyright (C) 2018-2024 Alexander Schmid
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.alexqp.timecontrol.command;

import com.github.alexqp.commons.command.AlexCommand;
import com.github.alexqp.commons.command.AlexSubCommand;
import com.github.alexqp.commons.config.ConfigChecker;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.MessageTranslator;
import com.github.alexqp.timecontrol.data.WorldContainer;
import com.github.alexqp.timecontrol.main.InternalsProvider;
import com.github.alexqp.timecontrol.main.TimeControl;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;

public class TimeControlCommand extends AlexCommand {

    public TimeControlCommand(@NotNull TimeControl plugin, @NotNull InternalsProvider internals, @NotNull WorldContainer worldContainer) {
        super("timecontrol", plugin, ChatColor.AQUA);

        ConfigChecker configChecker = new ConfigChecker(plugin);
        ConfigurationSection msgSection = configChecker.checkConfigSection(plugin.getConfig(), "messages.cmd", ConsoleErrorType.ERROR);
        if (msgSection != null) {
            String prefix = configChecker.checkString(msgSection, "prefix", ConsoleErrorType.WARN, "");
            assert prefix != null;
            if (!prefix.isEmpty())
                this.setPrefix(MessageTranslator.translateBukkitColorCodes(prefix));

            this.addCreditLine(MessageTranslator.translateBukkitColorCodes(Objects.requireNonNull(configChecker.checkString(msgSection, "credits", ConsoleErrorType.WARN, "Use /timecontrol help for all available commands."))));
            this.addHelpCmdHeaderLine(MessageTranslator.translateBukkitColorCodes(Objects.requireNonNull(configChecker.checkString(msgSection, "help_header", ConsoleErrorType.WARN, "List of all available commands:"))));
            this.setNoPermissionLine(MessageTranslator.translateBukkitColorCodes(Objects.requireNonNull(configChecker.checkString(msgSection, "noPerm", ConsoleErrorType.WARN, "&4You do not have permission."))));
            this.setUsagePrefix(MessageTranslator.translateBukkitColorCodes(Objects.requireNonNull(configChecker.checkString(msgSection, "wrongCmdUsagePrefix", ConsoleErrorType.WARN, "&CUsage:"))));

            HashSet<AlexSubCommand> subCmds = new HashSet<>();
            subCmds.add(new DebugSubCmd(this, plugin, worldContainer));
            subCmds.add(new ReloadSubCmd(this, plugin));
            subCmds.add(new UninstallSubCmd(this, plugin, internals, worldContainer));
            subCmds.add(new SetSubCmd(this, worldContainer));

            this.addSubCmds(subCmds);
            this.register();
        }
    }
}

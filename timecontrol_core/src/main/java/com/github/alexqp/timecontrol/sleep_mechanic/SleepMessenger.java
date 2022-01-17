package com.github.alexqp.timecontrol.sleep_mechanic;

import com.github.alexqp.commons.config.ConfigChecker;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;
import com.github.alexqp.commons.messages.MessageTranslator;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;

public class SleepMessenger {

    @NotNull
    public static SleepMessenger build(@NotNull JavaPlugin plugin) {
        ConfigChecker configChecker = new ConfigChecker(plugin);
        SleepMessenger sleepMessenger = new SleepMessenger(plugin);

        ConfigurationSection sleepingMsgSection = configChecker.checkConfigSection(plugin.getConfig(), "messages.sleeping", ConsoleErrorType.ERROR);
        if (sleepingMsgSection != null) {
            ConfigurationSection actionBarSection = configChecker.checkConfigSection(sleepingMsgSection, "actionBar", ConsoleErrorType.ERROR);
            if (actionBarSection != null) {
                if (configChecker.checkBoolean(actionBarSection, "enable", ConsoleErrorType.WARN, true)) {
                    ConfigurationSection msgSection = configChecker.checkConfigSection(actionBarSection, "msg", ConsoleErrorType.ERROR);
                    if (msgSection != null) {
                        sleepMessenger.actionBarMsgSuccess = MessageTranslator.translateBukkitColorCodes(Objects.requireNonNull(configChecker.checkString(msgSection, "success", ConsoleErrorType.WARN, "&eSweet dreams everyone!")));
                        sleepMessenger.actionBarMsgProgress = configChecker.checkString(msgSection, "progress", ConsoleErrorType.WARN, "&e%sleep_sleeping% of %sleep_needed% players are sleeping.");
                    }
                }
            }

            ConfigurationSection chatSection = configChecker.checkConfigSection(sleepingMsgSection, "chat", ConsoleErrorType.ERROR);
            if (chatSection != null) {
                if (configChecker.checkBoolean(chatSection, "enable", ConsoleErrorType.WARN, true)) {
                    ConfigurationSection msgSection = configChecker.checkConfigSection(chatSection, "msg", ConsoleErrorType.ERROR);
                    if (msgSection != null) {
                        sleepMessenger.enterBedMsg = configChecker.checkString(msgSection, "enterBed", ConsoleErrorType.WARN, "%player% is now sleeping. (%sleep_sleeping%/%sleep_needed%)");
                        sleepMessenger.leaveBedMsg = configChecker.checkString(msgSection, "leaveBed", ConsoleErrorType.WARN, "%player% left bed. (%sleep_sleeping%/%sleep_needed%)");
                    }
                }
            }
        }

        return sleepMessenger;
    }

    private final JavaPlugin plugin;
    private BaseComponent prefix = new TextComponent();

    private final String actionBarPermission = "timecontrol.sleep.actionbar";
    private final String chatPermission = "timecontrol.sleep.chat";

    private BaseComponent[] actionBarMsgSuccess;
    private String actionBarMsgProgress;
    private String enterBedMsg;
    private String leaveBedMsg;

    private final HashSet<String> msgCooldownWorlds = new HashSet<>();

    public SleepMessenger(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendSkipping(@NotNull World world) {
        if (actionBarMsgSuccess != null) {
            for (Player p : world.getPlayers()) {
                if (p.hasPermission(actionBarPermission))
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, actionBarMsgSuccess);
            }
        }
    }

    public void sendProgress(@NotNull World world, int needed, int sleeping) {
        if (actionBarMsgProgress != null) {
            BaseComponent[] msg = this.prepareMsg(actionBarMsgProgress, needed, sleeping, null);
            for (Player p : world.getPlayers()) {
                if (p.hasPermission(actionBarPermission))
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg);
            }
        }
    }

    public void sendBedEnterMsg(@NotNull Player p, int needed, int sleeping) {
        if (enterBedMsg != null && !this.isOnCooldown(p.getWorld())) {
            BaseComponent[] msg = this.prepareMsg(enterBedMsg, needed, sleeping, p);
            for (Player player : p.getWorld().getPlayers()) {
                if (player.hasPermission(chatPermission))
                    player.spigot().sendMessage(msg);
            }
        }
    }

    public void sendBedLeaveMsg(@NotNull Player p, int needed, int sleeping) {
        if (leaveBedMsg != null && !this.isOnCooldown(p.getWorld())) {
            BaseComponent[] msg = this.prepareMsg(leaveBedMsg, needed, sleeping, p);
            for (Player player : p.getWorld().getPlayers()) {
                if (player.hasPermission(chatPermission))
                    player.spigot().sendMessage(msg);
            }
        }
    }

    private BaseComponent[] prepareMsg(@NotNull String msg, int needed, int sleeping, @Nullable Player p) {
        String print = msg.replace("%sleep_needed%", "" + needed)
                .replace("%sleep_sleeping%", "" + sleeping);
        if (p != null) {
            print = print.replace("%player%", p.getDisplayName());
        } else {
            print = print.replace("%player%", "");
        }
        return new ComponentBuilder(prefix).append(" ").append(MessageTranslator.translateBukkitColorCodes(print)).create();
    }

    private boolean isOnCooldown(@Nullable World world) {
        return world != null && msgCooldownWorlds.contains(world.getName());
    }

    public void setMsgCooldown(@Nullable World world, int cooldown) {
        if (world != null && msgCooldownWorlds.add(world.getName())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    msgCooldownWorlds.remove(world.getName());
                }
            }.runTaskLater(plugin, cooldown);
            ConsoleMessage.debug(this.getClass(), plugin, "World " + world.getName() + " is on message cooldown for " + cooldown + " ticks.");
        }

    }

    public void setPrefix(@NotNull BaseComponent prefix) {
        this.prefix = prefix;
    }
}

package com.github.alexqp.timecontrol.main;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.github.alexqp.commons.config.ConsoleErrorType;
import com.github.alexqp.commons.messages.ConsoleMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class InternalsProvider {

    public InternalsProvider() {}

    public boolean handleGameRules(@NotNull World world) {
        return this.handleGameRules(world, false);
    }

    public boolean handleGameRules(@NotNull World world, boolean enable) {
        world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, enable ? 100 : 101);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, enable);
        return true;
    }

    public boolean needAllLeftBedTrigger() {
        return false;
    }

    public void disableSleepActionBar(@NotNull JavaPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.CHAT) {
                @Override
                public void onPacketSending(PacketEvent e) {
                    if (e.getPacketType().equals(PacketType.Play.Server.CHAT)) {
                        PacketContainer packet = e.getPacket();
                        for (WrappedChatComponent component : packet.getChatComponents().getValues()) {
                            if (component.getJson().contains("\"translate\":\"sleep.not_possible\"")) {
                                e.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            });
            ConsoleMessage.debug(this.getClass(), plugin, "Disabled Vanilla-Sleep-Messages");
        } else {
            ConsoleMessage.send(ConsoleErrorType.WARN, plugin, "ProtocolLib is not installed on your server. Vanilla-Sleep-Messages will not be disabled");
        }
    }
}

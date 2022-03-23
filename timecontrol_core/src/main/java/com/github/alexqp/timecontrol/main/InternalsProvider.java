package com.github.alexqp.timecontrol.main;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
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
        world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, enable ? 101 : 100);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, enable);
        return true;
    }

    public boolean needAllLeftBedTrigger() {
        return false;
    }

    public void disableSleepActionBar(@NotNull JavaPlugin plugin) {
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
    }
}

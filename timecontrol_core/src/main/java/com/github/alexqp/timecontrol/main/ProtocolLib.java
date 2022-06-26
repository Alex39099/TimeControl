package com.github.alexqp.timecontrol.main;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.github.alexqp.commons.messages.ConsoleMessage;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class ProtocolLib {

    public static void disableSleepActionBar(@NotNull JavaPlugin plugin) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.CHAT) {
            @Override
            public void onPacketSending(PacketEvent e) {
                if (e.getPacketType().equals(PacketType.Play.Server.CHAT)) {
                    PacketContainer packet = e.getPacket();
                    for (WrappedChatComponent component : packet.getChatComponents().getValues()) {
                        try {
                            if (component != null && component.getJson().contains("\"translate\":\"sleep.not_possible\"")) {
                                e.setCancelled(true);
                                return;
                            }
                        }
                        catch (NullPointerException ignored) {} // something with custom login messages does not work.
                    }
                }
            }
        });
        ConsoleMessage.debug(ProtocolLib.class, plugin, "Disabled Vanilla-Sleep-Messages");
    }
}

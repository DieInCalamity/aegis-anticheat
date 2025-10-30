package com.aegis.check;

import com.aegis.Aegis;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;

public class BadPacketsCheck extends CheckBase {

    private int lastSlot = -1;

    public BadPacketsCheck() {
        super("badpackets");

        if (Bukkit.getPluginManager().isPluginEnabled("PacketEvents") && Aegis.getInstance().isPacketEventsEnabled()) {
            registerPacketListener();
        } else {
            Aegis.getInstance().getLogger().warning("PacketEvents not found or not enabled — BadPackets check disabled.");
        }
    }

    private void registerPacketListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (!(event.getPlayer() instanceof Player)) return;
                Player p = (Player) event.getPlayer();

                if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;
                if (Aegis.getInstance().getConfigManager().isExempt(p)) return;

                if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
                    int slot = new WrapperPlayClientHeldItemChange(event).getSlot();

                    if (slot == lastSlot) {
                        fail(p, "duplicate held item slot");
                        event.setCancelled(true);
                    }

                    lastSlot = slot;
                }
            }
        });
    }
}
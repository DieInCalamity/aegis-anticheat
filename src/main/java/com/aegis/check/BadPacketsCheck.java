package com.aegis.check;

import com.aegis.Aegis;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BadPacketsCheck extends CheckBase {

    private final Map<UUID, Integer> lastSlotMap = new HashMap<>();

    public BadPacketsCheck() {
        super("badpackets");
        registerPacketListener();
    }

    private void registerPacketListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.HELD_ITEM_CHANGE) return;
                if (!(event.getPlayer() instanceof Player player)) return;

                if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;
                if (Aegis.getInstance().getConfigManager().isExempt(player)) return;

                WrapperPlayClientHeldItemChange wrapper = new WrapperPlayClientHeldItemChange(event);
                int slot = wrapper.getSlot();
                UUID uuid = player.getUniqueId();

                Integer lastSlot = lastSlotMap.get(uuid);
                if (lastSlot != null && lastSlot == slot) {
                    fail(player, "Duplicate held item slot (" + slot + ")");
                    event.setCancelled(true);
                }

                lastSlotMap.put(uuid, slot);
            }
        });
    }
}

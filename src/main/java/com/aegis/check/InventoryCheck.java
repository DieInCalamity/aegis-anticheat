package com.aegis.check;

import com.aegis.Aegis;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItem;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

public class InventoryCheck extends CheckBase {

    private int lastSlot = -1;
    private ItemStack lastItem;
    private long lastClick = 0;
    private boolean picking = false;

    public InventoryCheck() {
        super("inventory");

        if (Bukkit.getPluginManager().isPluginEnabled("PacketEvents")) {
            registerPacketListener();
        } else {
            Aegis.getInstance().getLogger().warning("PacketEvents not found — Inventory check disabled.");
        }
    }

    private void registerPacketListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (!(event.getPlayer() instanceof Player)) return;
                Player p = (Player) event.getPlayer();

                if (!cfg.isCheckEnabled(key)) return;
                if (cfg.isExempt(p)) return;

                if (cfg.getBoolean("checks.inventory.detect_sprint_inventory", true)
                        && event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW
                        && p.isSprinting()) {
                    WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);
                    fail(p, "moved item in inventory while sprinting");
                    event.setCancelled(true);
                    return;
                }

                if (cfg.getBoolean("checks.inventory.detect_instant_click", true)
                        && event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
                    WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);
                    long time = System.currentTimeMillis() - lastClick;

                    if (lastItem != null && lastItem.getType() == wrapper.getCarriedItemStack().getType()) {
                        lastSlot = wrapper.getSlot();
                        lastItem = wrapper.getCarriedItemStack();
                        lastClick = System.currentTimeMillis();
                        return;
                    }

                    if (time <= 1) {
                        fail(p, "instant click, slot=" + wrapper.getSlot());
                        event.setCancelled(true);
                    }

                    lastSlot = wrapper.getSlot();
                    lastItem = wrapper.getCarriedItemStack();
                    lastClick = System.currentTimeMillis();
                    return;
                }

                if (cfg.getBoolean("checks.inventory.detect_attack_with_inventory", true)
                        && event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                    WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
                    if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK
                            && p.getOpenInventory() != null) {
                        fail(p, "attack entity with inventory open");
                        event.setCancelled(true);
                        return;
                    }
                }

                if (event.getPacketType() == PacketType.Play.Client.PICK_ITEM) {
                    picking = true;
                }

                if ((event.getPacketType() == PacketType.Play.Client.USE_ITEM
                        || event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING)
                        && picking) {
                    String action = event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING
                            ? new WrapperPlayClientPlayerDigging(event).getAction().name()
                            : "use item";
                    fail(p, "instant action type=" + action);
                    event.setCancelled(true);
                }

                if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
                    long now = System.currentTimeMillis();
                    if (lastClick != 0 && now - lastClick < 3) {
                        if (cfg.getBoolean("checks.inventory.detect_fast_close", true)) {
                            fail(p, "fast close inventory, dt=" + (now - lastClick));
                            event.setCancelled(true);
                        }
                    }
                }
            }
        });
    }

    private int[] translatePosition(int slot) {
        int row = slot / 9 + 1;
        int rowPos = slot - (row - 1) * 9;
        return new int[]{ row, rowPos };
    }

    private double distanceBetween(int slot1, int slot2) {
        int[] s1 = translatePosition(slot1);
        int[] s2 = translatePosition(slot2);
        return Math.sqrt((s1[0] - s2[0]) * (s1[0] - s2[0])
                + (s1[1] - s2[1]) * (s1[1] - s2[1]));
    }
}

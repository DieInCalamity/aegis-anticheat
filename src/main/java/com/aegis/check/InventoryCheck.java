package com.aegis.check;

import com.aegis.Aegis;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItem;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryCheck extends CheckBase {

    private final Map<Player, Boolean> guiOpenMap = new ConcurrentHashMap<>();

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

                PacketType type = event.getPacketType();

                if (type == PacketType.Play.Client.CLICK_WINDOW) {
                    WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);

                    guiOpenMap.put(p, true);

                    if (cfg.getBoolean("checks.inventory.detect_sprint_inventory", true)
                            && p.isSprinting()) {
                        fail(p, "moved item in inventory while sprinting");
                        event.setCancelled(true);
                        return;
                    }

                    if (cfg.getBoolean("checks.inventory.detect_instant_click", true)) {
                        long time = System.currentTimeMillis() - lastClick;
                        ItemStack carried = wrapper.getCarriedItemStack();
                        if (lastItem != null && carried != null && lastItem.getType() == carried.getType()) {
                            lastSlot = wrapper.getSlot();
                            lastItem = carried;
                            lastClick = System.currentTimeMillis();
                            return;
                        }
                        if (time <= 1) {
                            fail(p, "instant click, slot=" + wrapper.getSlot());
                            event.setCancelled(true);
                        }
                        lastSlot = wrapper.getSlot();
                        lastItem = carried;
                        lastClick = System.currentTimeMillis();
                        return;
                    }
                }

                if (type == PacketType.Play.Client.CLOSE_WINDOW) {
                    WrapperPlayClientCloseWindow wrapper = new WrapperPlayClientCloseWindow(event);
                    guiOpenMap.put(p, false);

                    long now = System.currentTimeMillis();
                    if (lastClick != 0 && now - lastClick < 3) {
                        if (cfg.getBoolean("checks.inventory.detect_fast_close", true)) {
                            fail(p, "fast close inventory, dt=" + (now - lastClick));
                            event.setCancelled(true);
                        }
                    }
                    return;
                }

                if (type == PacketType.Play.Client.PICK_ITEM) {
                    picking = true;
                }

                if ((type == PacketType.Play.Client.USE_ITEM
                        || type == PacketType.Play.Client.PLAYER_DIGGING)
                        && picking) {

                    String action = (type == PacketType.Play.Client.PLAYER_DIGGING)
                            ? new WrapperPlayClientPlayerDigging(event).getAction().name()
                            : "use item";

                    fail(p, "instant action type=" + action);
                    event.setCancelled(true);
                    return;
                }

                if (cfg.getBoolean("checks.inventory.detect_attack_with_inventory", true)
                        && type == PacketType.Play.Client.INTERACT_ENTITY) {

                    WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
                    if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                        Boolean guiOpen = guiOpenMap.getOrDefault(p, false);
                        if (guiOpen) {
                            fail(p, "attack entity with inventory open");
                            event.setCancelled(true);
                            return;
                        }
                    }
                }

            }
        });
    }
}

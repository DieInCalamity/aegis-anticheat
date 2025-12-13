package com.aegis.check;

import com.aegis.Aegis;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;

import java.util.Collection;

public class HorizontalMotionCheck extends CheckBase implements Listener {
    public HorizontalMotionCheck() {
        super("horizontalmotion");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;
        Player p = e.getPlayer();
        if (Aegis.getInstance().getConfigManager().isExempt(p)) return;
        if (e.getTo() == null) return;
        
        boolean ignoreSpear = Aegis.getInstance().getConfigManager()
            .getBoolean("checks.horizontalmotion.ignore_spear_holders", false);
        if (ignoreSpear && hasSpear(p)) {
            return;
        }

        double dx = e.getTo().getX() - e.getFrom().getX();
        double dz = e.getTo().getZ() - e.getFrom().getZ();
        double speed = Math.hypot(dx, dz);

        double max = Aegis.getInstance().getConfigManager().getDouble("checks.horizontalmotion.max_speed", 0.9);
        int ignorePush = Aegis.getInstance().getConfigManager().getInt("checks.horizontalmotion.ignore_push", 1);
        double expand = Aegis.getInstance().getConfigManager().getDouble("checks.horizontalmotion.push_expand", 0.3);

        if (ignorePush == 1 && isPushedByEntity(p, expand)) {
            return;
        }

        if (speed > max && !p.isFlying() && !p.isGliding() && p.getFallDistance() < 1.5f) {
            fail(p, String.format("speed=%.3f max=%.2f", speed, max));
        }
    }
    
    public static boolean hasSpear(Player player) {
        PlayerInventory inv = player.getInventory();
        
        ItemStack mainHand = inv.getItemInMainHand();
        if (isSpearItem(mainHand)) {
            return true;
        }
        
        ItemStack offHand = inv.getItemInOffHand();
        if (isSpearItem(offHand)) {
            return true;
        }
        
        return false;
    }
    
    private static boolean isSpearItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        String itemName = item.getType().name().toLowerCase();
        
        if (itemName.endsWith("_spear")) {
            try {
                Material material = item.getType();
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }
    
    private boolean isPushedByEntity(Player p, double expand) {
        Collection<Entity> nearby = p.getWorld().getNearbyEntities(p.getBoundingBox().expand(expand));
        return false;
    }
}

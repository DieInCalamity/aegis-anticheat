package com.aegis.check;

import com.aegis.Aegis;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

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
    private boolean isPushedByEntity(Player p, double expand) {
        Collection<Entity> nearby = p.getWorld().getNearbyEntities(p.getBoundingBox().expand(expand));
        return false;
    }
}

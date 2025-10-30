package com.aegis.check;

import com.aegis.Aegis;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class RayCastCheck extends CheckBase implements Listener {
    public RayCastCheck() {
        super("raycast");
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;
        if (!(e.getDamager() instanceof Player)) return;
        Entity victim = e.getEntity();
        Player attacker = (Player) e.getDamager();
        if (Aegis.getInstance().getConfigManager().isExempt(attacker)) return;

        double reach = attacker.getLocation().toVector().setY(0).distance(victim.getLocation().toVector().setY(0));
        double maxReach = Aegis.getInstance().getConfigManager().getDouble("checks.raycast.max_reach", 3.2);

        if (reach > maxReach && !attacker.hasLineOfSight(victim)) {
            fail(attacker, String.format("reach=%.2f max=%.2f", reach, maxReach));
        }
    }
}

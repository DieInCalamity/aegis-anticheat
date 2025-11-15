package com.aegis.check;

import com.aegis.Aegis;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NoSlowCheck extends CheckBase implements Listener {

    private final Map<UUID, Integer> sneakGrace = new HashMap<>();
    private final Map<UUID, Integer> itemGrace = new HashMap<>();

    public NoSlowCheck() {
        super("noslow");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;
        Player p = e.getPlayer();
        if (Aegis.getInstance().getConfigManager().isExempt(p)) return;
        if (e.getTo() == null) return;

        UUID uuid = p.getUniqueId();

        if (p.isSneaking()) {
            if (!sneakGrace.containsKey(uuid)) {
                int grace = Aegis.getInstance().getConfigManager().getInt("checks.noslow.sneak.grace_ticks", 5);
                sneakGrace.put(uuid, grace);
            }
        }

        if (sneakGrace.containsKey(uuid)) {
            int ticks = sneakGrace.get(uuid);
            if (ticks > 0) {
                sneakGrace.put(uuid, ticks - 1);
                return;
            } else {
                sneakGrace.remove(uuid);
            }
        }

        boolean skipItemCheck = false;
        if (p.isHandRaised()) {
            if (!itemGrace.containsKey(uuid)) {
                int grace = Aegis.getInstance().getConfigManager().getInt("checks.noslow.items.grace_ticks", 5);
                itemGrace.put(uuid, grace);
            }
        } else {
            itemGrace.remove(uuid);
        }

        if (itemGrace.containsKey(uuid)) {
            int ticks = itemGrace.get(uuid);
            if (ticks > 0) {
                itemGrace.put(uuid, ticks - 1);
                skipItemCheck = true;
            } else {
                itemGrace.remove(uuid);
            }
        }

        double dx = e.getTo().getX() - e.getFrom().getX();
        double dz = e.getTo().getZ() - e.getFrom().getZ();
        double speed = Math.hypot(dx, dz);

        double baseSpeed = Aegis.getInstance().getConfigManager().getDouble("checks.noslow.base_speed", 0.7);
        double adjustedBase = baseSpeed;

        if (p.hasPotionEffect(PotionEffectType.SPEED) && p.getPotionEffect(PotionEffectType.SPEED) != null) {
            int amp = p.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
            adjustedBase *= (1.0 + 0.2 * amp);
        }

        if (p.hasPotionEffect(PotionEffectType.SLOWNESS)) return;

        double itemThreshold = Aegis.getInstance().getConfigManager()
                .getDouble("checks.noslow.items.min_slow_percent", 0.6);
        if (itemThreshold > 0 && p.isHandRaised() && !skipItemCheck) {
            double maxAllowed = itemThreshold * adjustedBase;
            if (speed > maxAllowed) {
                fail(p, String.format("noslow-item speed=%.3f max=%.3f", speed, maxAllowed));
            }
        }

        double sneakThreshold = Aegis.getInstance().getConfigManager()
                .getDouble("checks.noslow.sneak.min_slow_percent", 0.3);
        if (sneakThreshold > 0 && p.isSneaking()) {
            double maxAllowed = sneakThreshold * adjustedBase;
            if (speed > maxAllowed) {
                fail(p, String.format("noslow-sneak speed=%.3f max=%.3f", speed, maxAllowed));
            }
        }
    }
}

package com.aegis.check;

import com.aegis.Aegis;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import com.aegis.check.HorizontalMotionCheck;

import java.util.*;

public class VerticalMotionCheck extends CheckBase implements Listener {

    private final Map<UUID, Integer> waterGrace = new HashMap<>();
    private final Map<UUID, Deque<Double>> dyHistory = new HashMap<>();
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();
    private final Map<UUID, Long> lastTeleportTime = new HashMap<>();

    public VerticalMotionCheck() {
        super("verticalmotion");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;
        Player p = e.getPlayer();
        if (Aegis.getInstance().getConfigManager().isExempt(p)) return;
        if (e.getTo() == null) return;

        UUID uuid = p.getUniqueId();

        if (p.isInsideVehicle()) return;
        if (p.getAllowFlight() || p.isFlying()) return;
        GameMode gm = p.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;

        if (p.isGliding() || p.hasPotionEffect(PotionEffectType.LEVITATION) || p.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            resetAirData(p);
            return;
        }

        if (p.isSwimming() || p.getLocation().getBlock().isLiquid()) {
            int graceTicks = Aegis.getInstance().getConfigManager().getInt("checks.verticalmotion.water_grace_ticks", 20);
            waterGrace.put(uuid, graceTicks);
            resetAirData(p);
            return;
        }
        if (waterGrace.containsKey(uuid)) {
            int ticks = waterGrace.get(uuid);
            if (ticks > 0) {
                waterGrace.put(uuid, ticks - 1);
                resetAirData(p);
                return;
            } else {
                waterGrace.remove(uuid);
            }
        }

        double dy = e.getTo().getY() - e.getFrom().getY();
        Vector vel = p.getVelocity();
        long now = System.currentTimeMillis();

        double vclipThreshold = Aegis.getInstance().getConfigManager().getDouble("checks.verticalmotion.vclip_threshold_blocks", 5.0);
        long vclipMaxIntervalMs = Aegis.getInstance().getConfigManager().getInt("checks.verticalmotion.vclip_max_interval_ms", 300);
        long teleportGraceMs = Aegis.getInstance().getConfigManager().getInt("checks.verticalmotion.teleport_grace_ms", 500);

        Long tpTime = lastTeleportTime.get(uuid);
        if (tpTime != null && now - tpTime <= teleportGraceMs) {
        } else {
            long lastTime = lastMoveTime.getOrDefault(uuid, now);
            long deltaMs = Math.max(1, now - lastTime);

            if (Math.abs(dy) >= vclipThreshold && deltaMs <= vclipMaxIntervalMs) {
                boolean onClimbable = false;
                Block below = p.getLocation().getBlock().getRelative(0, -1, 0);
                if (below != null) {
                    Material m = below.getType();
                    if (m == Material.LADDER || m == Material.VINE || m == Material.SCAFFOLDING) onClimbable = true;
                }
                if (!onClimbable) {
                    fail(p, String.format("vclip detected: dy=%.3f blocks in %dms (threshold=%.1f)", dy, deltaMs, vclipThreshold));
                    resetAirData(p);
                    lastMoveTime.put(uuid, now);
                    return;
                }
            }
        }

        lastMoveTime.put(uuid, now);

        if (p.isOnGround() && dy <= 0.001) {
            boolean foundSolid = false;
            for (int x = -3; x <= 3 && !foundSolid; x++) {
                for (int z = -3; z <= 3; z++) {
                    Block block = p.getLocation().add(x, -1, z).getBlock();
                    if (block.getType().isSolid()) {
                        foundSolid = true;
                        break;
                    }
                }
            }
            if (!foundSolid) fail(p, "groundspoof: onGround=true but no solid block within 3-block radius below");
        }

        double maxStep = Aegis.getInstance().getConfigManager().getDouble("checks.verticalmotion.max_step_height", 0.6);
        if (dy > maxStep && p.isOnGround() && e.getFrom().getY() % 1.0 == 0) {
            fail(p, String.format("step dy=%.3f max=%.2f", dy, maxStep));
        }

        if (dy > 0) {
            boolean nearSolid = false;
            for (Block b : new Block[]{
                    p.getLocation().add(0.5, 0, 0).getBlock(),
                    p.getLocation().add(-0.5, 0, 0).getBlock(),
                    p.getLocation().add(0, 0, 0.5).getBlock(),
                    p.getLocation().add(0, 0, -0.5).getBlock()
            }) {
                Material type = b.getType();
                if (type.isSolid() && type != Material.LADDER && type != Material.VINE && type != Material.SCAFFOLDING) {
                    nearSolid = true;
                    break;
                }
            }
            double maxClimb = Aegis.getInstance().getConfigManager().getDouble("checks.verticalmotion.max_climb_speed", 0.35);
            if (nearSolid && dy > maxClimb && !p.isOnGround() && !p.getLocation().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                fail(p, String.format("spider dy=%.3f max=%.2f", dy, maxClimb));
            }
        }

        double maxJump = Aegis.getInstance().getConfigManager().getDouble("checks.verticalmotion.max_jump_height", 1.25);
        if (!p.isOnGround() && dy > maxJump) {
            fail(p, String.format("highjump dy=%.3f max=%.2f", dy, maxJump));
        }

        if (!p.isOnGround()) {
            dyHistory.putIfAbsent(uuid, new LinkedList<>());
            Deque<Double> history = dyHistory.get(uuid);
            history.addLast(dy);
            if (history.size() > 12) history.pollFirst();

            if (history.size() >= 6) {
                double avgDy = history.stream().mapToDouble(Math::abs).average().orElse(0.0);
                if (avgDy < 0.005) {
                    fail(p, String.format("fly/hover detected: avgDy=%.5f ticks=%d", avgDy, history.size()));
                    history.clear();
                }
            }

            if (dy < 0 && dy > -0.03) {
                fail(p, String.format("antigravity fall dy=%.3f", dy));
                history.clear();
            }

            double horizontalSpeed = Math.sqrt(vel.getX()*vel.getX() + vel.getZ()*vel.getZ());
            if (horizontalSpeed > 0.6 && dy >= -0.03) {
                if (ignoreSpear && hasSpear(p)) {
                    return;
                }
                fail(p, String.format("horizontal fly detected: speed=%.3f dy=%.3f", horizontalSpeed, dy));
                history.clear();
            }

        } else {
            resetAirData(p);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        if (Aegis.getInstance().getConfigManager().isExempt(p)) return;
        lastTeleportTime.put(p.getUniqueId(), System.currentTimeMillis());
    }

    private void resetAirData(Player p) {
        dyHistory.remove(p.getUniqueId());
    }
}

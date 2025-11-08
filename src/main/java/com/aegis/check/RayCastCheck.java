package com.aegis.check;

import com.aegis.Aegis;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;

public class RayCastCheck extends CheckBase {

    private final double maxReach = Aegis.getInstance().getConfigManager()
            .getDouble("checks.raycast.max_reach", 3.2);
    private final double predictionFactor = Aegis.getInstance().getConfigManager()
            .getDouble("checks.raycast.prediction_factor", 0.45);
    private final double leniency = Aegis.getInstance().getConfigManager()
            .getDouble("checks.raycast.leniency", 0.25);
    private final double rayEntitySize = Aegis.getInstance().getConfigManager()
            .getDouble("checks.raycast.ray_entity_size", 0.6);

    public RayCastCheck() {
        super("raycast");
        registerPacketListener();
    }

    private void registerPacketListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
                if (!(event.getPlayer() instanceof Player player)) return;

                if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;
                if (Aegis.getInstance().getConfigManager().isExempt(player)) return;

                WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
                if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

                int targetId = wrapper.getEntityId();

                Player victim = null;
                for (Player online : player.getWorld().getPlayers()) {
                    if (online.getEntityId() == targetId) {
                        victim = online;
                        break;
                    }
                }
                if (victim == null) return;

                Vector predictedCenter = victim.getLocation().toVector()
                        .add(victim.getVelocity().clone().multiply(predictionFactor))
                        .add(new Vector(0, (victim.getHeight() > 0 ? victim.getHeight() : 1.8) / 2.0, 0));

                Vector attackerEyes = player.getEyeLocation().toVector();
                Vector dir = predictedCenter.clone().subtract(attackerEyes);
                double distance = dir.length();
                if (distance <= 1e-6) return;
                Vector dirNorm = dir.clone().normalize();

                if (distance <= maxReach + leniency) return;

                Location startLoc = player.getEyeLocation();
                RayTraceResult blockHit = player.getWorld().rayTraceBlocks(
                        startLoc,
                        dirNorm,
                        distance,
                        FluidCollisionMode.NEVER
                );

                boolean obstructed = false;
                if (blockHit != null && blockHit.getHitPosition() != null) {
                    double blockDist = blockHit.getHitPosition().toVector().distance(attackerEyes);
                    if (blockDist < distance - 1e-6) obstructed = true;
                }

                if (obstructed) return;

                RayTraceResult entityHit = player.getWorld().rayTraceEntities(
                        startLoc,
                        dirNorm,
                        distance,
                        rayEntitySize,
                        e -> e.getEntityId() == victim.getEntityId()
                );

                boolean hitVictim = entityHit != null && entityHit.getHitEntity() != null
                        && entityHit.getHitEntity().getEntityId() == victim.getEntityId();

                if (hitVictim || !hitVictim) {
                    if (!player.hasLineOfSight(victim)) {
                        fail(player, String.format("reach=%.2f max=%.2f leniency=%.2f victim=%s",
                                distance, maxReach, leniency, victim.getName()));
                        event.setCancelled(true);
                    } else {
                        fail(player, String.format("reach=%.2f max=%.2f leniency=%.2f victim=%s (lineOfSight)",
                                distance, maxReach, leniency, victim.getName()));
                        event.setCancelled(true);
                    }
                }
            }
        });
    }
}

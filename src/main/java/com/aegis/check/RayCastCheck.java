package com.aegis.check;

import com.aegis.Aegis;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;

public class RayCastCheck extends CheckBase {

    private final double maxReach;
    private final double predictionFactor;
    private final double leniency;
    private final double rayEntitySize;
    private final double maxAllowed;

    public RayCastCheck() {
        super("raycast");
        this.maxReach = Aegis.getInstance().getConfigManager()
                .getDouble("checks.raycast.max_reach", 3.2);
        this.predictionFactor = Aegis.getInstance().getConfigManager()
                .getDouble("checks.raycast.prediction_factor", 0.45);
        this.leniency = Aegis.getInstance().getConfigManager()
                .getDouble("checks.raycast.leniency", 0.25);
        this.rayEntitySize = Aegis.getInstance().getConfigManager()
                .getDouble("checks.raycast.ray_entity_size", 0.6);
        this.maxAllowed = this.maxReach + this.leniency;
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
                if (!player.getWorld().equals(victim.getWorld())) return;

                Vector victimVel = victim.getVelocity();
                double victimHeight = victim.getHeight() > 0 ? victim.getHeight() : 1.8;
                Vector predictedCenter = victim.getLocation().toVector()
                        .add(victimVel.clone().multiply(predictionFactor))
                        .add(new Vector(0, victimHeight / 2.0, 0));

                Vector attackerEyes = player.getEyeLocation().toVector();
                Vector direction = predictedCenter.clone().subtract(attackerEyes);
                double distance = direction.length();
                if (distance <= 1e-6) return;
                Vector dirNorm = direction.clone().normalize();

                if (distance <= maxAllowed) return;

                Location startLoc = player.getEyeLocation();
                RayTraceResult blockHit = player.getWorld().rayTraceBlocks(
                        startLoc,
                        dirNorm,
                        distance,
                        FluidCollisionMode.NEVER
                );

                if (blockHit != null && blockHit.getHitPosition() != null) {
                    double blockDist = blockHit.getHitPosition().distance(attackerEyes);
                    if (blockDist < distance - 1e-6) return;
                }

                final int victimId = victim.getEntityId();
                RayTraceResult entityHit = player.getWorld().rayTraceEntities(
                        startLoc,
                        dirNorm,
                        distance,
                        rayEntitySize,
                        e -> e.getEntityId() == victimId
                );

                boolean hitVictim = entityHit != null && entityHit.getHitEntity() != null
                        && entityHit.getHitEntity().getEntityId() == victimId;

                if (!hitVictim) {
                    if (!player.hasLineOfSight(victim)) {
                        fail(player, String.format("reach=%.2f max=%.2f leniency=%.2f victim=%s",
                                distance, maxReach, leniency, victim.getName()));
                    } else {
                        fail(player, String.format("reach=%.2f max=%.2f leniency=%.2f victim=%s (lineOfSight)",
                                distance, maxReach, leniency, victim.getName()));
                    }
                    event.setCancelled(true);
                }
            }
        });
    }
}

package com.aegis.check;

import com.aegis.Aegis;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class RayCastCheck extends CheckBase {

    private final double maxReach = Aegis.getInstance().getConfigManager()
            .getDouble("checks.raycast.max_reach", 3.2);

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

                Vector victimPredicted = victim.getLocation().toVector().add(victim.getVelocity());

                Vector attackerEyes = player.getEyeLocation().toVector();
                double reach = attackerEyes.distance(victimPredicted.add(new Vector(0, victim.getHeight() / 2.0, 0)));

                if (reach > maxReach && !player.hasLineOfSight(victim)) {
                    fail(player, String.format("reach=%.2f max=%.2f", reach, maxReach));
                    event.setCancelled(true);
                }
            }
        });
    }
}

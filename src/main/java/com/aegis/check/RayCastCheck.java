package com.aegis.check;

import com.aegis.Aegis;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RayCastCheck extends CheckBase {

    private final double maxReach = Aegis.getInstance().getConfigManager()
            .getDouble("checks.raycast.max_reach", 3.2);

    public RayCastCheck() {
        super("raycast");

        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                    handle(event);
                }
            }
        });
    }

    private void handle(PacketReceiveEvent event) {
        if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;

        Player attacker = (Player) event.getPlayer();
        if (attacker == null || Aegis.getInstance().getConfigManager().isExempt(attacker)) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        org.bukkit.entity.Entity victimBukkit = Bukkit.getEntity(wrapper.getEntityId());
        if (!(victimBukkit instanceof Player victim)) return;

        Vector3d attackerEyes = new Vector3d(
                attacker.getEyeLocation().getX(),
                attacker.getEyeLocation().getY(),
                attacker.getEyeLocation().getZ()
        );

        Vector3d victimCenter = new Vector3d(
                victim.getLocation().getX(),
                victim.getLocation().getY() + victim.getHeight() / 2.0,
                victim.getLocation().getZ()
        );

        double distance = attackerEyes.distance(victimCenter);
        if (distance > maxReach) {
            fail(attacker, String.format("reach=%.3f max=%.2f", distance, maxReach));
        }
    }
}

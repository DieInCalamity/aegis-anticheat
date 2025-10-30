package com.aegis.check;

import com.aegis.Aegis;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.impl.PacketPlayUseEntityEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RayCastCheck extends CheckBase {

    private final double maxReach = Aegis.getInstance().getConfigManager()
            .getDouble("checks.raycast.max_reach", 3.2);

    public RayCastCheck() {
        super("raycast");

        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketPlayUseEntity(PacketPlayUseEntityEvent event) {
                handle(event);
            }
        });
    }

    private void handle(PacketPlayUseEntityEvent event) {
        if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;

        User user = event.getUser();
        Player attacker = (Player) user.getPlayer();
        if (attacker == null || Aegis.getInstance().getConfigManager().isExempt(attacker)) return;

        if (event.getEntityType() != EntityType.PLAYER) return;

        WrapperPlayClientUseEntity.Action action = event.getAction();
        if (action != WrapperPlayClientUseEntity.Action.ATTACK) return;

        Location eyeLoc = user.getLocation();
        Vector3d direction = user.getPosition().getDirection();

        org.bukkit.entity.Entity victimBukkit = Bukkit.getEntity(event.getEntityId());
        if (!(victimBukkit instanceof Player victim)) return;

        Vector3d victimPos = new Vector3d(
                victim.getLocation().getX(),
                victim.getLocation().getY() + victim.getHeight() / 2.0,
                victim.getLocation().getZ()
        );

        double distance = eyeLoc.toVector3d().distance(victimPos);

        boolean withinReach = distance <= maxReach;

        if (!withinReach) {
            fail(attacker, String.format("reach=%.3f max=%.2f", distance, maxReach));
        }
    }
}

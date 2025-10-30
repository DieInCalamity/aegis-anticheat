package com.aegis.check;

import com.aegis.Aegis;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
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
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() == PacketType.Play.Client.USE_ENTITY) {
                    handle(event);
                }
            }
        });
    }

    private void handle(PacketReceiveEvent event) {
        if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;

        Player attacker = (Player) event.getPlayer();
        if (attacker == null || Aegis.getInstance().getConfigManager().isExempt(attacker)) return;

        WrapperPlayClientUseEntity wrapper = new WrapperPlayClientUseEntity(event);
        if (wrapper.getAction() != WrapperPlayClientUseEntity.Action.ATTACK) return;

        org.bukkit.entity.Entity victimBukkit = Bukkit.getEntity(wrapper.getEntityId());
        if (!(victimBukkit instanceof Player victim)) return;

        Location eyeLoc = new Location(
                attacker.getWorld().getName(),
                attacker.getEyeLocation().getX(),
                attacker.getEyeLocation().getY(),
                attacker.getEyeLocation().getZ(),
                attacker.getLocation().getYaw(),
                attacker.getLocation().getPitch()
        );

        Vector3d victimCenter = new Vector3d(
                victim.getLocation().getX(),
                victim.getLocation().getY() + victim.getHeight() / 2.0,
                victim.getLocation().getZ()
        );

        double distance = eyeLoc.getPosition().distance(victimCenter);

        if (distance > maxReach) {
            fail(attacker, String.format("reach=%.3f max=%.2f", distance, maxReach));
        }
    }
}

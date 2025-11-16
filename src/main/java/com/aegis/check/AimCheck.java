package com.aegis.check;

import com.aegis.Aegis;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import org.bukkit.entity.Player;

import java.util.*;

public class AimCheck extends CheckBase {

    private final Map<UUID, RotData> dataMap = new HashMap<>();

    public AimCheck() {
        super("aimcheck");
        registerPacketListener();
    }

    private RotData get(UUID id) {
        return dataMap.computeIfAbsent(id, k -> new RotData());
    }

    private void registerPacketListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (!(event.getPlayer() instanceof Player player)) return;
                if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;
                if (Aegis.getInstance().getConfigManager().isExempt(player)) return;

                UUID uuid = player.getUniqueId();
                RotData d = get(uuid);
                PacketType.Play.Client type = (PacketType.Play.Client) event.getPacketType();

                if (type == PacketType.Play.Client.ANIMATION) {
                    d.lastAttack = System.currentTimeMillis();
                    d.wasAttackThisTick = true;
                }

                if (type == PacketType.Play.Client.PLAYER_ROTATION) {
                    WrapperPlayClientPlayerRotation w = new WrapperPlayClientPlayerRotation(event);
                    handle(player, d, w.getYaw(), w.getPitch());
                }

                if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                    WrapperPlayClientPlayerPositionAndRotation w = new WrapperPlayClientPlayerPositionAndRotation(event);
                    handle(player, d, w.getYaw(), w.getPitch());
                }
            }
        });
    }

    private void handle(Player p, RotData d, float yaw, float pitch) {
        double dy = Math.abs(wrap(yaw - d.lastYaw));
        double dp = Math.abs(pitch - d.lastPitch);
        boolean suspicious = dy > 35 || dp > 35 || (dy > 10 && dp > 10);

        if (suspicious) d.preBuffer.add(System.currentTimeMillis());
        if (d.preBuffer.size() > 3) d.preBuffer.removeFirst();

        if (d.wasAttackThisTick) {
            for (long t : d.preBuffer) {
                if (System.currentTimeMillis() - t <= 180) {
                    fail(p, "Pre-attack snap");
                    break;
                }
            }
        }

        if (d.wasAttackThisTick) {
            d.postTicks = 0;
        } else {
            if (d.postTicks < 2 && suspicious && System.currentTimeMillis() - d.lastAttack < 250) {
                fail(p, "Post-attack snap");
            }
            d.postTicks++;
        }

        d.wasAttackThisTick = false;
        d.lastYaw = yaw;
        d.lastPitch = pitch;
    }

    private float wrap(float v) {
        v %= 360f;
        if (v >= 180f) v -= 360f;
        if (v < -180f) v += 360f;
        return v;
    }

    private static class RotData {
        float lastYaw = 0;
        float lastPitch = 0;
        boolean wasAttackThisTick = false;
        long lastAttack = 0;
        int postTicks = 100;
        Deque<Long> preBuffer = new ArrayDeque<>();
    }
}

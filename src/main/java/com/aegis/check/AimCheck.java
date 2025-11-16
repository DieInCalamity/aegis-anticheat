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
        double maxYawChange = Aegis.getInstance().getConfigManager().getDouble("checks.aimcheck.max-yaw-change", 35.0);
        double maxPitchChange = Aegis.getInstance().getConfigManager().getDouble("checks.aimcheck.max-pitch-change", 35.0);
        double combinedThreshold = Aegis.getInstance().getConfigManager().getDouble("checks.aimcheck.combined-threshold", 10.0);
        
        long preAttackTime = (long) Aegis.getInstance().getConfigManager().getInt("checks.aimcheck.pre-attack-time", 180);
        long postAttackTime = (long) Aegis.getInstance().getConfigManager().getInt("checks.aimcheck.post-attack-time", 250);
        long violationResetTime = (long) Aegis.getInstance().getConfigManager().getInt("checks.aimcheck.violation-reset-time", 5000);
        
        int bufferSize = Aegis.getInstance().getConfigManager().getInt("checks.aimcheck.buffer-size", 3);
        int postTicksThreshold = Aegis.getInstance().getConfigManager().getInt("checks.aimcheck.post-ticks-threshold", 2);
        boolean detectPreAttack = Aegis.getInstance().getConfigManager().getBoolean("checks.aimcheck.detect-pre-attack", true);
        boolean detectPostAttack = Aegis.getInstance().getConfigManager().getBoolean("checks.aimcheck.detect-post-attack", true);
        double minDelta = Aegis.getInstance().getConfigManager().getDouble("checks.aimcheck.min-delta", 0.1);
        boolean resetOnTeleport = Aegis.getInstance().getConfigManager().getBoolean("checks.aimcheck.reset-on-teleport", true);
        boolean exemptCreative = Aegis.getInstance().getConfigManager().getBoolean("checks.aimcheck.exempt-creative", true);
        int violationsToAlert = Aegis.getInstance().getConfigManager().getInt("checks.aimcheck.violations-to-alert", 2);

        if (exemptCreative && p.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        double dy = Math.abs(wrap(yaw - d.lastYaw));
        double dp = Math.abs(pitch - d.lastPitch);
        
        if (dy < minDelta && dp < minDelta) {
            d.lastYaw = yaw;
            d.lastPitch = pitch;
            return;
        }

        boolean suspicious = dy > maxYawChange || dp > maxPitchChange || (dy > combinedThreshold && dp > combinedThreshold);

        if (suspicious) d.preBuffer.add(System.currentTimeMillis());
        if (d.preBuffer.size() > bufferSize) d.preBuffer.removeFirst();

        if (detectPreAttack && d.wasAttackThisTick) {
            for (long t : d.preBuffer) {
                if (System.currentTimeMillis() - t <= preAttackTime) {
                    d.violations++;
                    if (d.violations >= violationsToAlert) {
                        fail(p, String.format("Pre-attack snap (Δy=%.2f, Δp=%.2f)", dy, dp));
                        d.violations = 0;
                    }
                    break;
                }
            }
        }
        if (detectPostAttack) {
            if (d.wasAttackThisTick) {
                d.postTicks = 0;
            } else {
                if (d.postTicks < postTicksThreshold && suspicious && System.currentTimeMillis() - d.lastAttack < postAttackTime) {
                    d.violations++;
                    if (d.violations >= violationsToAlert) {
                        fail(p, String.format("Post-attack snap (Δy=%.2f, Δp=%.2f)", dy, dp));
                        d.violations = 0;
                    }
                }
                d.postTicks++;
            }
        }

        if (System.currentTimeMillis() - d.lastViolationReset > violationResetTime) {
            d.violations = Math.max(0, d.violations - 1);
            d.lastViolationReset = System.currentTimeMillis();
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

    public void resetPlayer(Player player) {
        boolean resetOnTeleport = Aegis.getInstance().getConfigManager().getBoolean("checks.aimcheck.reset-on-teleport", true);
        if (resetOnTeleport) {
            dataMap.remove(player.getUniqueId());
        }
    }

    private static class RotData {
        float lastYaw = 0;
        float lastPitch = 0;
        boolean wasAttackThisTick = false;
        long lastAttack = 0;
        int postTicks = 100;
        int violations = 0;
        long lastViolationReset = System.currentTimeMillis();
        Deque<Long> preBuffer = new ArrayDeque<>();
    }
}

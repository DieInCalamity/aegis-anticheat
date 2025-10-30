package com.aegis.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    public long lastBalanceTick = 0L;
    public double balance = 0.0;
    public int movesThisTick = 0;

    private final UUID uuid;
    private final Map<String, Integer> violations = new HashMap<>();

    public long lastBreakTime = 0L;
    public long lastSwingMillis = 0L;
    public int swingsThisSecond = 0;
    public long swingSecondStart = System.currentTimeMillis();
    public long lastMoveSecond = System.currentTimeMillis();
    public int movesThisSecond = 0;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public int addViolation(String check) {
        int vl = violations.getOrDefault(check, 0) + 1;
        violations.put(check, vl);
        return vl;
    }

    public int getVl(String check) {
        return violations.getOrDefault(check, 0);
    }

    public void resetVl(String check) {
        violations.remove(check);
    }

    public UUID getUuid() {
        return uuid;
    }
}

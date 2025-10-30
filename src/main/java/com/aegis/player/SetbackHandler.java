package com.aegis.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SetbackHandler {

    private final Map<UUID, Location> lastLegit = new HashMap<>();

    public void update(Player p, Location loc) {
        lastLegit.put(p.getUniqueId(), loc.clone());
    }

    public void setback(Player p) {
        Location loc = lastLegit.get(p.getUniqueId());
        if (loc != null) {
            p.teleportAsync(loc);
        }
    }
}

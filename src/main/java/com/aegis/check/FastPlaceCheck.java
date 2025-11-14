package com.aegis.check;

import com.aegis.Aegis;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FastPlaceCheck extends CheckBase implements Listener {

    private final Map<UUID, Long> lastPlace = new HashMap<>();
    private final Map<UUID, Integer> placeCount = new HashMap<>();
    private final Map<UUID, Long> burstStart = new HashMap<>();
    private final Map<UUID, Integer> violations = new HashMap<>();

    public FastPlaceCheck() {
        super("fastplace");
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;

        Player p = e.getPlayer();
        if (Aegis.getInstance().getConfigManager().isExempt(p)) return;

        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();

        // minum delay in config (i dont quite understand your configuration)
        int minDelay = Aegis.getInstance().getConfigManager()
                .getInt("checks.fastplace.min_delay_ms", 60); // antes 90

        // Burst max in 1 second
        int maxBurst = Aegis.getInstance().getConfigManager()
                .getInt("checks.fastplace.max_burst", 20); // 20 blocks in 1s = permisive and configurable

        // Inicializer
        burstStart.putIfAbsent(id, now);
        placeCount.putIfAbsent(id, 0);

        // its for testing only
        if (now - burstStart.get(id) > 1000) {
            burstStart.put(id, now);
            placeCount.put(id, 0);
        }

        placeCount.put(id, placeCount.get(id) + 1);

        if (placeCount.get(id) > maxBurst) {
            int vl = violations.getOrDefault(id, 0) + 2;
            violations.put(id, vl);

            fail(p, String.format("fastplace-burst %d > %d (vl=%d)", placeCount.get(id), maxBurst, vl));
        }

        // Check delay between places
        if (lastPlace.containsKey(id)) {
            long diff = now - lastPlace.get(id);

            // only for 0 ms place (cheating, obviously)
            if (diff < minDelay / 2) {  // ultra very fast => hack
                int vl = violations.getOrDefault(id, 0) + 1;
                violations.put(id, vl);

                fail(p, String.format("fastplace delay=%dms < %dms (vl=%d)", diff, minDelay, vl));
            }
        }

        lastPlace.put(id, now);
    }
}

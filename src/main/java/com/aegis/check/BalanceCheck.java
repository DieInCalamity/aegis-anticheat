package com.aegis.check;

import com.aegis.Aegis;
import com.aegis.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class BalanceCheck extends CheckBase implements Listener {
    public BalanceCheck() {
        super("balance");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;
        Player p = e.getPlayer();
        if (Aegis.getInstance().getConfigManager().isExempt(p)) return;

        PlayerData data = Aegis.getInstance().getData(p);
        long now = System.currentTimeMillis();

        if (now - data.lastMoveSecond >= 1000) {
            data.movesThisSecond = 0;
            data.lastMoveSecond = now;
        }
        data.movesThisSecond++;

        int maxMps = Aegis.getInstance().getConfigManager().getInt("checks.balance.max_moves_per_second", 25);
        if (data.movesThisSecond > maxMps) {
            fail(p, "mps=" + data.movesThisSecond + " max=" + maxMps);
        }
    }
}
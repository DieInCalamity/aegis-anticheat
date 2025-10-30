package com.aegis.check;

import com.aegis.Aegis;
import com.aegis.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;

public class AutoClickerCheck extends CheckBase implements Listener {
    public AutoClickerCheck() {
        super("autoclicker");
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent e) {
        if (!Aegis.getInstance().getConfigManager().isCheckEnabled(key)) return;
        Player p = e.getPlayer();
        if (Aegis.getInstance().getConfigManager().isExempt(p)) return;

        PlayerData data = Aegis.getInstance().getData(p);
        long now = System.currentTimeMillis();
        if (now - data.swingSecondStart >= 1000) {
            data.swingsThisSecond = 0;
            data.swingSecondStart = now;
        }
        data.swingsThisSecond++;

        int cps = Aegis.getInstance().getConfigManager().getInt("checks.autoclicker.cps_threshold", 16);
        if (data.swingsThisSecond > cps) {
            fail(p, "cps=" + data.swingsThisSecond + " max=" + cps);
        }
    }
}

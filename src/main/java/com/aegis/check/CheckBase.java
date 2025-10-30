package com.aegis.check;

import com.aegis.Aegis;
import com.aegis.config.ConfigManager;
import com.aegis.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

public abstract class CheckBase {

    protected final String key;
    protected final ConfigManager cfg;

    protected CheckBase(String key) {
        this.key = key;
        this.cfg = Aegis.getInstance().getConfigManager();
    }

    protected boolean fail(Player player, String debug) {
        if (!cfg.isCheckEnabled(key)) return false;

        PlayerData data = Aegis.getInstance().getData(player);
        int vl = data.addViolation(key);

        Aegis.getInstance().getAlertManager().alert(player, key, vl, debug);

        int setbackVl = cfg.getSetbackVl(key);
        if (cfg.isSetbackEnabled() && setbackVl != -1 && vl >= setbackVl) {
            Aegis.getInstance().getSetbackHandler().setback(player);
        }

        Map<Integer, String> cmds = cfg.getCommands(key);
        for (Map.Entry<Integer, String> e : cmds.entrySet()) {
            if (vl >= e.getKey()) {
                String cmd = e.getValue().replace("%player%", player.getName());
                Bukkit.getScheduler().runTask(Aegis.getInstance(), () ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                );
            }
        }

        return true;
    }
}

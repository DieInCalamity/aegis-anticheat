package com.aegis.config;

import com.aegis.Aegis;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final Aegis plugin;
    private FileConfiguration cfg;

    public ConfigManager(Aegis plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    public boolean isAlertsEnabledByDefault() {
        return cfg.getBoolean("settings.alerts", true);
    }

    public boolean isSetbackEnabled() {
        return cfg.getBoolean("settings.setback_enabled", true);
    }

    public boolean isCheckEnabled(String check) {
        return cfg.getBoolean("checks." + check + ".enabled", true);
    }

    public int getSetbackVl(String check) {
        return cfg.getInt("checks." + check + ".setback_vl", -1);
    }

    public Map<Integer, String> getCommands(String check) {
        Map<Integer, String> map = new HashMap<>();
        ConfigurationSection sec = cfg.getConfigurationSection("checks." + check + ".commands");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    int vl = Integer.parseInt(key);
                    map.put(vl, sec.getString(key));
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    public boolean isExempt(Player p) {
        String gm = p.getGameMode().name();
        if (gm.equals(GameMode.CREATIVE.name()) && cfg.getStringList("settings.exempt").contains("CREATIVE")) return true;
        if (gm.equals(GameMode.SPECTATOR.name()) && cfg.getStringList("settings.exempt").contains("SPECTATOR")) return true;
        if (p.isGliding() && cfg.getStringList("settings.exempt").contains("ELYTRA")) return true;
        if (p.getVehicle() instanceof Boat && cfg.getStringList("settings.exempt").contains("BOAT")) return true;
        return false;
    }

    public FileConfiguration raw() {
        return cfg;
    }

    public double getDouble(String path, double def) {
        return cfg.getDouble(path, def);
    }

    public int getInt(String path, int def) { return cfg.getInt(path, def); }
    public boolean getBoolean(String path, boolean def) {
        return cfg.getBoolean(path, def);
    }
}

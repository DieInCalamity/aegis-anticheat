package com.aegis;

import com.aegis.check.*;
import com.aegis.command.AegisCommand;
import com.aegis.config.ConfigManager;
import com.aegis.player.PlayerData;
import com.aegis.player.SetbackHandler;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Aegis extends JavaPlugin implements Listener {

    private static Aegis instance;
    private ConfigManager configManager;
    private SetbackHandler setbackHandler;
    private AlertManager alertManager;
    private final Map<UUID, PlayerData> dataMap = new HashMap<>();
    private boolean packetEventsEnabled = false;

    public static Aegis getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.setbackHandler = new SetbackHandler();
        this.alertManager = new AlertManager();

        Bukkit.getPluginManager().registerEvents(this, this);

        registerCheck(new HorizontalMotionCheck());
        registerCheck(new VerticalMotionCheck());
        registerCheck(new BalanceCheck());
        registerCheck(new NoSlowCheck());
        registerCheck(new RayCastCheck());
        registerCheck(new FastBreakCheck());
        registerCheck(new AutoClickerCheck());
        registerCheck(new ChatCheck());
        new BadPacketsCheck();
        new InventoryCheck();

        setupPacketEvents();

        getCommand("aegis").setExecutor(new AegisCommand());
        getLogger().info("Aegis AntiCheat enabled" + (packetEventsEnabled ? " with PacketEvents." : "."));
    }

    @Override
    public void onDisable() {
        if (packetEventsEnabled) {
            PacketEvents.getAPI().terminate();
        }
        getLogger().info("Aegis AntiCheat disabled.");
    }

    private void registerCheck(Listener check) {
        Bukkit.getPluginManager().registerEvents(check, this);
    }

    private void setupPacketEvents() {
        Plugin pe = Bukkit.getPluginManager().getPlugin("PacketEvents");
        if (pe == null) {
            getLogger().warning("PacketEvents not found — packet‑level checks will be disabled.");
            return;
        }

        try {
            PacketEvents.getAPI().load();
            PacketEvents.getAPI().init();
            packetEventsEnabled = true;
            getLogger().info("Hooked into PacketEvents successfully.");
        } catch (Exception ex) {
            getLogger().warning("Failed to initialize PacketEvents: " + ex.getMessage());
        }
    }

    public boolean isPacketEventsEnabled() {
        return packetEventsEnabled;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SetbackHandler getSetbackHandler() {
        return setbackHandler;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public PlayerData getData(Player p) {
        return dataMap.computeIfAbsent(p.getUniqueId(), k -> new PlayerData(p.getUniqueId()));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        dataMap.put(e.getPlayer().getUniqueId(), new PlayerData(e.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        dataMap.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (getConfigManager().isExempt(p)) return;

        if (!p.isFlying()
                && !p.isGliding()
                && !(p.getVehicle() instanceof Boat)
                && p.getGameMode().name().equals("SURVIVAL")) {

            double dx = e.getTo().getX() - e.getFrom().getX();
            double dz = e.getTo().getZ() - e.getFrom().getZ();
            double dy = e.getTo().getY() - e.getFrom().getY();
            double horiz = Math.hypot(dx, dz);
            if (horiz <= 0.9 && dy < 1.0) {
                Location loc = e.getFrom().clone();
                setbackHandler.update(p, loc);
            }
        }
    }
}

package com.aegis;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class AlertManager {

    private final Set<String> alertsOn = new HashSet<>();

    public boolean toggleAlerts(CommandSender sender) {
        String name = sender.getName().toLowerCase();
        if (alertsOn.contains(name)) {
            alertsOn.remove(name);
            sender.sendMessage(ChatColor.YELLOW + "[Aegis] Alerts disabled.");
            return false;
        } else {
            alertsOn.add(name);
            sender.sendMessage(ChatColor.GREEN + "[Aegis] Alerts enabled.");
            return true;
        }
    }

    public void alert(Player suspect, String check, int vl, String debug) {
        if (alertsOn.isEmpty()) return;
        String msg = ChatColor.RED + "[Aegis] " + ChatColor.WHITE + suspect.getName()
                + ChatColor.GRAY + " flagged " + ChatColor.AQUA + check.toUpperCase()
                + ChatColor.GRAY + " VL=" + ChatColor.GOLD + vl
                + ChatColor.DARK_GRAY + " (" + debug + ")";
        for (String name : alertsOn) {
            Player p = Bukkit.getPlayerExact(name);
            if (p != null) p.sendMessage(msg);
        }
        Bukkit.getLogger().info("[Aegis] " + suspect.getName() + " flagged " + check + " VL=" + vl + " (" + debug + ")");
    }
}

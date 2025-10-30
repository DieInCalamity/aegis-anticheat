package com.aegis.command;

import com.aegis.Aegis;
import com.aegis.config.ConfigManager;
import com.aegis.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AegisCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aegis.command")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/aegis alerts " + ChatColor.GRAY + "- toggle alerts");
            sender.sendMessage(ChatColor.YELLOW + "/aegis reload " + ChatColor.GRAY + "- reload config");
            sender.sendMessage(ChatColor.YELLOW + "/aegis vl <player> " + ChatColor.GRAY + "- show VLs");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("alerts")) {
            Aegis.getInstance().getAlertManager().toggleAlerts(sender);
            return true;
        }
        if (sub.equals("reload")) {
            Aegis.getInstance().getConfigManager().reload();
            sender.sendMessage(ChatColor.GREEN + "Aegis config reloaded.");
            return true;
        }
        if (sub.equals("vl")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /aegis vl <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            PlayerData data = Aegis.getInstance().getData(target);
            sender.sendMessage(ChatColor.AQUA + "VL for " + target.getName() + ": "
                    + ChatColor.GRAY + "(use logs/alerts for detailed values)");
            return true;
        }
        return true;
    }
}

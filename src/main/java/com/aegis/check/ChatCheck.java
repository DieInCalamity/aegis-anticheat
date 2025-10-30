package com.aegis.check;

import com.aegis.Aegis;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;

public class ChatCheck extends CheckBase implements Listener {

    private final boolean packetEventsAvailable;

    public ChatCheck() {
        super("chat");
        this.packetEventsAvailable = Bukkit.getPluginManager().isPluginEnabled("PacketEvents");
        if (packetEventsAvailable) registerPacketListener();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!cfg.isCheckEnabled(key)) return;

        Player p = e.getPlayer();
        if (cfg.isExempt(p)) return;

        boolean shouldCancel = false;

        if (cfg.getBoolean("checks.chat.detect_sprint_chat", true)) {
            if (p.isSprinting()) shouldCancel = fail(p, "chat while sprinting");
        }

        if (shouldCancel) {
            Bukkit.getScheduler().runTask(Aegis.getInstance(), () -> e.setCancelled(true));
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!cfg.isCheckEnabled(key)) return;

        Player p = e.getPlayer();
        if (cfg.isExempt(p)) return;

        boolean shouldCancel = false;

        if (cfg.getBoolean("checks.chat.detect_sprint_command", true)) {
            if (p.isSprinting()) shouldCancel = fail(p, "command while sprinting");
        }

        if (shouldCancel) {
            Bukkit.getScheduler().runTask(Aegis.getInstance(), () -> e.setCancelled(true));
        }
    }

    private void registerPacketListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (!(event.getPlayer() instanceof Player)) return;

                Player p = (Player) event.getPlayer();
                if (!cfg.isCheckEnabled(key)) return;
                if (cfg.isExempt(p)) return;
                if (!cfg.getBoolean("checks.chat.invalid_completion", true)) return;

                if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
                    WrapperPlayClientTabComplete packet = new WrapperPlayClientTabComplete(event);
                    if (PacketEvents.getAPI().getPlayerManager().getClientVersion(p).isNewerThanOrEquals(ClientVersion.V_1_13)) {
                        String text = packet.getText();
                        if (text.equals("/") || text.trim().isEmpty()) {
                            fail(p, "invalid tab completion");
                            event.setCancelled(true);
                        }
                    }
                }
            }
        });
    }
}

package org.rlaxogh76.randomEventsSurvival;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class PlayerJoinListener implements Listener {
    private final Plugin plugin;

    public PlayerJoinListener(Plugin plugin) {
        this.plugin = plugin;
    }

    String[] welcomeMessages = {
            "님이 착륙하셨어요.",
            "님이 서버에 막 등장하셨어요.",
            "님이 서버에 입장하셨어요.",
            "님이 서버에 등장!",
            "님 반가워요, 피자는 가져오셨겠죠?",
            "님이 서버에 입장하셨어요. 환영합니다!",
    };

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(
                org.bukkit.ChatColor.YELLOW + player.getName() + org.bukkit.ChatColor.YELLOW + " "
                        + welcomeMessages[(int) (Math.random() * welcomeMessages.length)]);
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(
                org.bukkit.ChatColor.YELLOW + player.getName() + org.bukkit.ChatColor.YELLOW + " 님이 서버를 떠났습니다.");
    }
}

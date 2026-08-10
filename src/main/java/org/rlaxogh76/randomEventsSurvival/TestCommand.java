package org.rlaxogh76.randomEventsSurvival;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class TestCommand implements CommandExecutor {

    private final RandomEventsSurvival plugin;

    public TestCommand(RandomEventsSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if(sender instanceof Player player) {
            sender.sendMessage("플러그인 테스트 성공!");
        } else {
            sender.sendMessage("플레이어만 사용 가능합니다.");
        }

        return true;
    }
}

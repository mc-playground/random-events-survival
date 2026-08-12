package org.rlaxogh76.randomEventsSurvival;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TitleCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String @NotNull [] args) {

        if (sender instanceof Player player) {
            player.sendTitle("타이틀 테스트", "서브타이틀 테스트", 10, 70, 20);
        } else {
            sender.sendMessage("플레이어만 명령어 실행이 가능합니다.");
        }

        return true;
    }

}

package org.rlaxogh76.randomEventsSurvival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// /이벤트 <이벤트키> [대상 플레이어]
// 대상 플레이어를 생략하면 명령어를 실행한 본인이 대상이 됩니다.
// 콘솔에서 실행할 경우에는 대상 플레이어를 반드시 지정해야 합니다.

public class EventCommand implements CommandExecutor, TabCompleter {

    private final EventListener eventListener;

    public EventCommand(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "사용법: /" + label + " <이벤트키> [대상 플레이어]");
            sender.sendMessage(ChatColor.GRAY + "사용 가능한 이벤트: " + String.join(", ", eventListener.getEventKeys()));
            return true;
        }

        String eventKey = args[0];

        if (!eventListener.getEventKeys().contains(eventKey)) {
            sender.sendMessage(ChatColor.RED + "알 수 없는 이벤트 키입니다: " + eventKey);
            sender.sendMessage(ChatColor.GRAY + "사용 가능한 이벤트: " + String.join(", ", eventListener.getEventKeys()));
            return true;
        }

        Player target;

        if (args.length >= 2) {
            // 대상 플레이어가 명시된 경우
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "플레이어를 찾을 수 없습니다: " + args[1]);
                return true;
            }
        } else {
            // 대상 생략 -> 명령어 실행자 본인
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "콘솔에서는 대상 플레이어를 반드시 지정해야 합니다.");
                sender.sendMessage(ChatColor.GRAY + "사용법: /" + label + " <이벤트키> <대상 플레이어>");
                return true;
            }
            target = player;
        }

        eventListener.trigger(eventKey, target);

        sender.sendMessage(ChatColor.GREEN + "[" + eventKey + "] 이벤트를 " + target.getName() + "님에게 실행했습니다.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            completions.addAll(
                    eventListener.getEventKeys().stream()
                            .filter(key -> key.toLowerCase().startsWith(input))
                            .collect(Collectors.toList()));
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}
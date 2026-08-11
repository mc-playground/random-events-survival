package org.rlaxogh76.randomEventsSurvival;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class GameStart implements CommandExecutor {

    private static final int EVENT_INTERVAL_SECONDS = 60; // 이벤트 주기(초)

    private final Plugin plugin;
    private BossBar eventBossBar;
    private BukkitRunnable eventTimerTask;
    private int secondsLeft;

    public GameStart(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String @NotNull [] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 명령어 실행이 가능합니다.");
            return true;
        }

        // plugin.yml 에 등록된 명령어 이름(label)에 맞춰 분기
        // 예: 게임시작 -> 타이머 시작, 종료 -> 타이머 정지
        switch (label.toLowerCase()) {
            case "게임시작" -> startGame();
            case "종료" -> stopGame();
            default -> {
                return false;
            }
        }

        return true;
    }

    private void startGame() {
        // 이미 실행 중인 타이머가 있으면 정리 후 새로 시작
        if (eventTimerTask != null) {
            eventTimerTask.cancel();
            eventTimerTask = null;
        }
        clearBossBar(eventBossBar);

        secondsLeft = EVENT_INTERVAL_SECONDS;

        eventBossBar = Bukkit.createBossBar(formatTitle(secondsLeft), BarColor.RED, BarStyle.SOLID);
        eventBossBar.setProgress(1.0);
        for (Player p : Bukkit.getOnlinePlayers()) {
            eventBossBar.addPlayer(p);
        }

        Bukkit.broadcastMessage("다음 이벤트까지 : " + secondsLeft + "초");

        eventTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                secondsLeft--;

                if (secondsLeft <= 0) {
                    // 여기에 실제 랜덤 이벤트 발생 로직을 넣으면 됩니다.
                    Bukkit.broadcastMessage("이벤트 발생!");

                    // 타이머를 처음부터 다시 시작 (0초가 되면 리셋)
                    secondsLeft = EVENT_INTERVAL_SECONDS;
                    Bukkit.broadcastMessage("다음 이벤트까지 : " + secondsLeft + "초");
                }

                eventBossBar.setTitle(formatTitle(secondsLeft));
                eventBossBar.setProgress((double) secondsLeft / EVENT_INTERVAL_SECONDS);
            }
        };

        // 20틱(1초) 후 시작, 이후 20틱(1초)마다 반복 실행
        eventTimerTask.runTaskTimer(plugin, 20L, 20L);
    }

    private void stopGame() {
        // 실행 중인 타이머가 있을 때만 종료
        if (eventTimerTask != null) {
            eventTimerTask.cancel();
            eventTimerTask = null;
        }

        // 보스바를 모든 플레이어에게서 제거
        if (eventBossBar != null) {
            clearBossBar(eventBossBar);
            eventBossBar = null;
        }

        secondsLeft = 0;

        Bukkit.broadcastMessage("이벤트 타이머가 종료되었습니다.");
    }

    private String formatTitle(int secondsLeft) {
        int min = secondsLeft / 60;
        int sec = secondsLeft % 60;
        return String.format("남은 시간: %02d:%02d", min, sec);
    }

    private void clearBossBar(BossBar bossBar) {
        if (bossBar == null)
            return;
        bossBar.removeAll(); // 보스바에 표시된 모든 플레이어 제거
    }
}
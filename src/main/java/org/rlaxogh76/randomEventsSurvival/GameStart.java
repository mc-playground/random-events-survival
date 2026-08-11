package org.rlaxogh76.randomEventsSurvival;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class GameStart implements CommandExecutor, Listener {

    private static final int EVENT_INTERVAL_SECONDS = 5; // 이벤트 주기(초) 기본 : 90초

    private final Plugin plugin;
    private BossBar eventBossBar;
    private BukkitRunnable eventTimerTask;
    private int secondsLeft;

    // 게임이 "켜져 있는 상태"인지(명령어로 시작했는지)와
    // 타이머가 "일시정지 중"인지를 구분해서 관리
    private boolean gameActive = false;
    private boolean paused = false;

    public GameStart(Plugin plugin) {
        this.plugin = plugin;
        // 리스너 등록 (main 클래스에서 이미 등록한다면 이 줄은 지워도 됩니다)
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String @NotNull [] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 명령어 실행이 가능합니다.");
            return true;
        }

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
        if (eventTimerTask != null) {
            eventTimerTask.cancel();
            eventTimerTask = null;
        }

        clearBossBar(eventBossBar);

        secondsLeft = EVENT_INTERVAL_SECONDS;
        gameActive = true;
        paused = false;

        eventBossBar = Bukkit.createBossBar(formatTitle(secondsLeft), BarColor.RED, BarStyle.SOLID);
        eventBossBar.setProgress(1.0);
        for (Player p : Bukkit.getOnlinePlayers()) {
            eventBossBar.addPlayer(p);
        }

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            Bukkit.broadcastMessage("서버에 플레이어가 없으므로 이벤트 타이머를 일시정지합니다.");
            paused = true;
            return; // 타이머는 시작하지 않고, 플레이어 접속 시 resumeGame()에서 시작
        }

        runTimer();
    }

    private void runTimer() {
        eventTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                secondsLeft--;

                if (secondsLeft <= 0) {

                    final String[] default_events = {
                            "item_remove", "tick_speed_change", "player_hp_change", "time_change", "hotbar_change",
                            "player_random_effect_give", "spawn_tnt"
                    };

                    final String[] double_events = {
                            "item_remove", "player_hp_change", "hotbar_change", "spawn_random_mob", "inventory_mix",
                            "spawn_tnt", "random_effect_give"
                    };

                    final String[] rare_events = {
                            "dragon_get_hp", "spawn_bob"
                    };

                    String selectedEvent;
                    double randomValue = Math.random();

                    if (randomValue < 0.01) {
                        selectedEvent = rare_events[(int) (Math.random() * rare_events.length)];
                    } else if (randomValue < 0.06) {
                        selectedEvent = double_events[(int) (Math.random() * double_events.length)];
                    } else {
                        selectedEvent = default_events[(int) (Math.random() * default_events.length)];
                    }

                    Bukkit.broadcastMessage("선택된 이벤트: " + selectedEvent);

                    secondsLeft = EVENT_INTERVAL_SECONDS;
                    Bukkit.broadcastMessage("다음 이벤트까지 : " + secondsLeft + "초");
                }

                eventBossBar.setTitle(formatTitle(secondsLeft));
                eventBossBar.setProgress((double) secondsLeft / EVENT_INTERVAL_SECONDS);
            }
        };

        eventTimerTask.runTaskTimer(plugin, 20L, 20L);
    }

    private void pauseGame() {
        if (eventTimerTask != null) {
            eventTimerTask.cancel();
            eventTimerTask = null;
        }
        paused = true;
        Bukkit.broadcastMessage("서버에 플레이어가 없어 이벤트 타이머를 일시정지합니다.");
        // secondsLeft, eventBossBar 상태는 그대로 유지 (초기화하지 않음)
    }

    private void resumeGame() {
        if (!gameActive || !paused)
            return;

        paused = false;
        Bukkit.broadcastMessage("플레이어가 접속하여 이벤트 타이머를 재개합니다. 남은 시간: " + secondsLeft + "초");
        runTimer();
    }

    private void stopGame() {
        if (eventTimerTask != null) {
            eventTimerTask.cancel();
            eventTimerTask = null;
        }

        if (eventBossBar != null) {
            clearBossBar(eventBossBar);
            eventBossBar = null;
        }

        secondsLeft = 0;
        gameActive = false;
        paused = false;

        Bukkit.broadcastMessage("이벤트 타이머가 종료되었습니다.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (eventBossBar != null) {
            eventBossBar.addPlayer(event.getPlayer());
        }
        if (gameActive && paused) {
            resumeGame();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (eventBossBar != null) {
            eventBossBar.removePlayer(event.getPlayer());
        }
        // 퇴장 처리 전이라 getOnlinePlayers()에 아직 이 플레이어가 포함되어 있으므로 1 빼고 계산
        int remaining = Bukkit.getOnlinePlayers().size() - 1;
        if (gameActive && !paused && remaining <= 0) {
            pauseGame();
        }
    }

    private String formatTitle(int secondsLeft) {
        int min = secondsLeft / 60;
        int sec = secondsLeft % 60;
        return String.format("남은 시간: %02d:%02d", min, sec);
    }

    private void clearBossBar(BossBar bossBar) {
        if (bossBar == null)
            return;
        bossBar.removeAll();
    }
}
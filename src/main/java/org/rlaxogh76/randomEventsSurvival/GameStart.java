package org.rlaxogh76.randomEventsSurvival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameStart implements CommandExecutor, Listener {

    private static final int EVENT_INTERVAL_SECONDS = 120; // 이벤트 주기(초) 기본 : 120초

    private static final int ROULETTE_TOTAL_STEPS = 20;
    private static final long ROULETTE_MIN_DELAY = 1L;
    private static final long ROULETTE_MAX_DELAY = 8L;
    private static final long ROULETTE_GAP_DELAY = 30L;

    private static final String[] DEFAULT_EVENTS = { // 일반 이벤트 목록
            "item_remove", "tick_speed_change", "player_hp_change", "time_change", "hotbar_change",
            "player_random_effect_give", "spawn_tnt", "yeet", "spawn_random_mob", "freeze_player", "firework",
            "tamed_wolf", "set_spawn", "block_remove",
            "poop", "burn_player"
    };

    // private static final String[] DOUBLE_EVENTS = { // 더블 이벤트 목록
    // "item_remove", "player_hp_change", "hotbar_change", "spawn_random_mob",
    // "inventory_mix",
    // "spawn_tnt", "random_effect_give"
    // };

    private static final String[] RARE_EVENTS = { // 희귀 이벤트 목록
            "dragon_get_hp", "spawn_bob"
    };

    private final Plugin plugin;
    private BossBar eventBossBar;
    private BukkitRunnable eventTimerTask;
    private int secondsLeft;

    // 게임이 "켜져 있는 상태"인지(명령어로 시작했는지)와
    // 타이머가 "일시정지 중"인지를 구분해서 관리
    private boolean gameActive = false;
    private boolean paused = false;
    private boolean playerRouletteRunning = false;
    private boolean eventRouletteRunning = false;

    private final EventListener eventListener;
    private Player selectedPlayer;

    public GameStart(Plugin plugin, EventListener eventListener) {
        this.plugin = plugin;
        this.eventListener = eventListener;
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

        eventBossBar = Bukkit.createBossBar(formatTitle(secondsLeft), BarColor.YELLOW, BarStyle.SOLID);
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
                    eventTimerTask.cancel();
                    eventTimerTask = null;

                    eventBossBar.setTitle(formatTitle(0));
                    eventBossBar.setProgress(0.0);

                    // 온라인 플레이어 중 한 명을 룰렛으로 뽑아 타이틀/채팅으로 공지
                    startPlayerRoulette(() -> Bukkit.getScheduler().runTaskLater(plugin,
                            () -> {
                                if (!gameActive)
                                    return; // 대기 중 "종료" 명령이 들어온 경우 중단
                                startEventRoulette(() -> {
                                    if (!gameActive || eventBossBar == null)
                                        return; // 진행 중 "종료" 명령이 들어온 경우 중단
                                    secondsLeft = EVENT_INTERVAL_SECONDS;
                                    eventBossBar.setTitle(formatTitle(secondsLeft));
                                    eventBossBar.setProgress(1.0);
                                    Bukkit.broadcastMessage("다음 이벤트까지 : " + secondsLeft + "초");
                                    if (gameActive && !paused) {
                                        runTimer();
                                    }
                                });
                            },
                            ROULETTE_GAP_DELAY));
                    return;
                }

                eventBossBar.setTitle(formatTitle(secondsLeft));
                eventBossBar.setProgress((double) secondsLeft / EVENT_INTERVAL_SECONDS);
            }
        };

        eventTimerTask.runTaskTimer(plugin, 20L, 20L);
    }

    // 당첨자 룰렛 관련 메서드들

    private void startPlayerRoulette(Runnable onComplete) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (players.isEmpty() || playerRouletteRunning) {
            // 플레이어가 없거나 이미 룰렛이 진행 중이면 스킵
            onComplete.run();
            return;
        }

        playerRouletteRunning = true;

        // 당첨자를 미리 정해두고, 룰렛은 마지막에 이 사람에서 멈추도록 함
        Player winner = players.get((int) (Math.random() * players.size()));

        runPlayerSelectRouletteStep(players, winner, 0, onComplete);
    }

    private void runPlayerSelectRouletteStep(List<Player> players, Player winner, int step, Runnable onComplete) {

        if (!gameActive) { // 게임이 종료된 경우 룰렛 중단
            playerRouletteRunning = false;
            return;
        }

        boolean isFinalStep = step >= ROULETTE_TOTAL_STEPS;

        // 마지막 스텝이 아니면 매번 무작위 플레이어를, 마지막이면 당첨자를 표시
        Player displayed = isFinalStep ? winner : players.get((int) (Math.random() * players.size()));

        String coloredName = randomColoredText(displayed.getName());

        for (Player p : Bukkit.getOnlinePlayers()) {
            // fadeIn 0틱, stay를 다음 회전 전까지 유지, fadeOut 짧게 -> 이름이 빠르게 전환되는 느낌
            p.sendTitle(ChatColor.YELLOW + "이번 이벤트 당첨자는", coloredName, 0, 12, 4);
        }

        if (isFinalStep) {
            playerRouletteRunning = false;

            selectedPlayer = winner; // ← 당첨자 저장

            Bukkit.broadcastMessage(
                    ChatColor.GOLD + "★ 이번 이벤트 당첨자는 " + coloredName + ChatColor.GOLD + " 님입니다! ★");

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            onComplete.run();
            return;
        }

        // step이 진행될수록(0 -> TOTAL) 딜레이가 점점 커지도록 3제곱 곡선 사용
        // (초반에는 거의 안 늘어나다가, 후반부에 급격히 느려짐 -> 룰렛이 멈추는 느낌)
        double progress = (double) step / ROULETTE_TOTAL_STEPS;
        long delay = ROULETTE_MIN_DELAY
                + Math.round(Math.pow(progress, 3) * (ROULETTE_MAX_DELAY - ROULETTE_MIN_DELAY));

        Bukkit.getScheduler().runTaskLater(plugin,
                () -> runPlayerSelectRouletteStep(players, winner, step + 1, onComplete),
                delay);
    }

    private void startEventRoulette(Runnable onComplete) {
        if (eventRouletteRunning) {
            onComplete.run();
            return;
        }

        eventRouletteRunning = true;

        String winner = pickWeightedEvent();

        List<String> pool = new ArrayList<>();
        pool.addAll(Arrays.asList(DEFAULT_EVENTS));
        // pool.addAll(Arrays.asList(DOUBLE_EVENTS));
        pool.addAll(Arrays.asList(RARE_EVENTS));

        runEventRouletteStep(pool, winner, 0, onComplete);
    }

    private String pickWeightedEvent() {
        double randomValue = Math.random();

        // 이벤트 확률 가중치

        if (randomValue < 0.01) { // 1% 확률로 희귀 이벤트
            return RARE_EVENTS[(int) (Math.random() * RARE_EVENTS.length)];
        }
        // else if (randomValue < 0.06) {
        // return DOUBLE_EVENTS[(int) (Math.random() * DOUBLE_EVENTS.length)];
        // }
        else { // 나머지 99% 확률로 일반 이벤트
            return DEFAULT_EVENTS[(int) (Math.random() * DEFAULT_EVENTS.length)];
        }
    }

    private void runEventRouletteStep(List<String> pool, String winner, int step, Runnable onComplete) {

        if (!gameActive) { // 게임이 종료된 경우 룰렛 중단
            eventRouletteRunning = false;
            return;
        }

        boolean isFinalStep = step >= ROULETTE_TOTAL_STEPS;

        String displayed = isFinalStep ? winner : pool.get((int) (Math.random() * pool.size()));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(ChatColor.AQUA + "이벤트는", randomColoredText(displayed), 0, 12, 4);
        }

        if (isFinalStep) {
            eventRouletteRunning = false;

            Bukkit.broadcastMessage(ChatColor.AQUA + "선택된 이벤트: " + randomColoredText(winner));

            eventListener.trigger(winner, selectedPlayer);

            onComplete.run();
            return;
        }

        double progress = (double) step / ROULETTE_TOTAL_STEPS;
        long delay = ROULETTE_MIN_DELAY
                + Math.round(Math.pow(progress, 3) * (ROULETTE_MAX_DELAY - ROULETTE_MIN_DELAY));

        Bukkit.getScheduler().runTaskLater(plugin,
                () -> runEventRouletteStep(pool, winner, step + 1, onComplete),
                delay);
    }

    private String randomColoredText(String text) {
        Color color = new Color((int) (Math.random() * 0x1000000));
        String hex = String.format("%06x", color.getRGB() & 0xFFFFFF);

        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        sb.append(text);

        return sb.toString();
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
        playerRouletteRunning = false;
        eventRouletteRunning = false;

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
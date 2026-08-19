package org.rlaxogh76.randomEventsSurvival;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class RandomEventsSurvival extends JavaPlugin {

    @Override
    public void onEnable() {
        Objects.requireNonNull(this.getCommand("test")).setExecutor(new TestCommand(this));
        EventListener eventListener = new EventListener(this);
        GameStart gameStart = new GameStart(this, eventListener);
        Objects.requireNonNull(this.getCommand("게임시작")).setExecutor(gameStart);
        Objects.requireNonNull(this.getCommand("종료")).setExecutor(gameStart);
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

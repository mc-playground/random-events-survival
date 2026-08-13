package org.rlaxogh76.randomEventsSurvival;

import java.util.List;

import org.bukkit.entity.Player;

public interface GameEvent {
    void execute(List<Player> players);
}

package com.fx.srp.managers.gamemodes;

import com.fx.srp.commands.SRPCommand;
import org.bukkit.entity.Player;

public interface IGameModeManager<T> {

    void handleCommand(Player player, SRPCommand command);

    void start(Player player);

    void reset(T speedrun, Player player);

    void stop(T speedrun, Player player);
}

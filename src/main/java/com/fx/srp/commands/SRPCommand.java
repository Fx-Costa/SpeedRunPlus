package com.fx.srp.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
@AllArgsConstructor
public class SRPCommand {

    public static String SRP = "srp";

    private final GameMode gameMode;
    private final Action action;
    private final Player targetPlayer;  // Optional

    /* ==========================================================
     *                     COMMAND BUILDER
     * ========================================================== */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private GameMode gameMode;
        private Action action;
        private Player targetPlayer;

        public Builder gameMode(GameMode gameMode) {
            this.gameMode = gameMode;
            return this;
        }

        public Builder action(Action action) {
            this.action = action;
            return this;
        }

        public Builder targetPlayer(Player player) {
            this.targetPlayer = player;
            return this;
        }

        public SRPCommand build() {
            if (gameMode == null) throw new IllegalStateException("GameMode is required");
            if (action == null) throw new IllegalStateException("Action is required");
            if (!gameMode.isValidAction(action))
                throw new IllegalStateException("Action " + action.getName() +
                        " does not belong to game mode " + gameMode.getName());

            return new SRPCommand(gameMode, action, targetPlayer);
        }
    }

    @Override
    public String toString() {
        String targetPlayerName = this.targetPlayer != null ? this.targetPlayer.getName() : "";
        return "/srp " + gameMode.getName() + " " + action.getName() + targetPlayerName;
    }
}


package com.fx.srp.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Represents a fully parsed SRP command, consisting of a {@link GameMode},
 * an {@link Action}, and optionally a target {@link Player}.
 *
 * <p>This class is created by the {@link SRPCommandParser} and used
 * by the command handler to determine what operation a player intends to execute.</p>
 *
 * <p>Instances are immutable and should be constructed via the
 * {@link SRPCommand.Builder}.</p>
 */
@Getter
@AllArgsConstructor
public class SRPCommand {

    private static final String SRP = "srp";

    private final GameMode gameMode;
    private final Action action;
    private final Player targetPlayer;

    public static String getSrp() {
        return SRP;
    }

    /* ==========================================================
     *                     COMMAND BUILDER
     * ========================================================== */
    /**
     * Creates a new builder for constructing {@link SRPCommand} instances.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating validated {@link SRPCommand} instances.
     *
     * <p>Ensures that:</p>
     * <ul>
     *     <li>A {@link GameMode} is provided</li>
     *     <li>An {@link Action} is provided</li>
     *     <li>The action belongs to the selected game mode</li>
     * </ul>
     *
     * <p>The resulting command is immutable.</p>
     */
    public static class Builder {
        private GameMode gameMode;
        private Action action;
        private Player targetPlayer;

        /**
         * Sets the game mode for the command.
         *
         * @param gameMode the game mode to use
         * @return this builder for method chaining
         */
        public Builder withGameMode(GameMode gameMode) {
            this.gameMode = gameMode;
            return this;
        }

        /**
         * Sets the action for the command.
         *
         * @param action the action to perform
         * @return this builder for method chaining
         */
        public Builder withAction(Action action) {
            this.action = action;
            return this;
        }

        /**
         * Sets the target player for the command, if required.
         *
         * @param player the player targeted by the command, or {@code null}
         * @return this builder for method chaining
         */
        public Builder withTargetPlayer(Player player) {
            this.targetPlayer = player;
            return this;
        }

        /**
         * Builds a validated {@link SRPCommand}.
         *
         * <p>Validation rules:</p>
         * <ul>
         *     <li>The game mode must be specified</li>
         *     <li>The action must be specified</li>
         *     <li>The action must be valid for the selected game mode</li>
         * </ul>
         *
         * @return a fully constructed and validated {@link SRPCommand}
         * @throws IllegalStateException if required fields are missing or incompatible
         */
        public SRPCommand build() {
            if (action == null) throw new IllegalStateException("Action is required");
            if (gameMode != null && !gameMode.isValidAction(action))
                throw new IllegalStateException("Action " + action.getName() +
                        " does not belong to game mode " + gameMode.getName());

            return new SRPCommand(gameMode, action, targetPlayer);
        }
    }

    @Override
    public String toString() {
        String targetPlayerName = this.targetPlayer != null ? this.targetPlayer.getName() : "";
        String gameModeName = this.gameMode != null ? this.gameMode.getName() : "";
        return "/srp " + gameModeName + " " + action.getName() + targetPlayerName;
    }
}


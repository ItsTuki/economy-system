package cl.auryx.economy.config;

import org.bukkit.configuration.file.FileConfiguration;

/** Valores de {@code config.yml} ya validados. */
public record Settings(
        int maxStackSize,
        int maxCommandAmount,
        boolean dropWhenFull,
        boolean notifyTargets,
        boolean logTransactions,
        boolean refreshOnJoin,
        boolean exchangeEnabled,
        boolean condenseEnabled,
        boolean payEnabled,
        double payMaxDistance,
        boolean droppedNeverDespawn,
        boolean droppedInvulnerable,
        Protection protection
) {
    public record Protection(
            boolean crafting,
            boolean crafter,
            boolean workstations,
            boolean furnaces,
            boolean villagerTrades,
            boolean placing,
            boolean consuming
    ) {
    }

    public static Settings from(FileConfiguration config) {
        return new Settings(
                clamp(config.getInt("coins.max-stack-size", 99), 1, 99),
                Math.max(1, config.getInt("commands.max-amount", 10_000)),
                config.getBoolean("commands.drop-when-full", true),
                config.getBoolean("commands.notify-targets", true),
                config.getBoolean("commands.log-transactions", true),
                config.getBoolean("coins.refresh-on-join", true),
                config.getBoolean("player-commands.exchange", true),
                config.getBoolean("player-commands.condense", true),
                config.getBoolean("player-commands.pay.enabled", true),
                config.getDouble("player-commands.pay.max-distance", 0),
                config.getBoolean("dropped-coins.never-despawn", true),
                config.getBoolean("dropped-coins.invulnerable", true),
                new Protection(
                        config.getBoolean("protection.crafting", true),
                        config.getBoolean("protection.crafter", true),
                        config.getBoolean("protection.workstations", true),
                        config.getBoolean("protection.furnaces", true),
                        config.getBoolean("protection.villager-trades", true),
                        config.getBoolean("protection.placing", true),
                        config.getBoolean("protection.consuming", true)
                )
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

package cl.auryx.economy.command;

import cl.auryx.economy.EconomyPlugin;
import cl.auryx.economy.currency.CurrencyDefinition;
import cl.auryx.economy.currency.CurrencyManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Utilidades compartidas por /economy y /coins. */
final class CommandSupport {
    private final EconomyPlugin plugin;

    CommandSupport(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    CurrencyManager coins() {
        return plugin.currencies();
    }

    SuggestionProvider<CommandSourceStack> currencySuggestions() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String id : coins().suggestionIds()) {
                if (id.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(id);
                }
            }
            return builder.buildFuture();
        };
    }

    /** Lee un argumento de moneda; si no existe avisa al usuario y devuelve vacío. */
    Optional<CurrencyDefinition> currency(CommandContext<CommandSourceStack> ctx, String argument) {
        String input = StringArgumentType.getString(ctx, argument);
        Optional<CurrencyDefinition> def = coins().find(input);
        if (def.isEmpty()) {
            plugin.messages().send(sender(ctx), "errors.unknown-currency", Placeholder.unparsed("input", input));
        }
        return def;
    }

    boolean checkAmount(CommandContext<CommandSourceStack> ctx, long amount) {
        int max = plugin.settings().maxCommandAmount();
        if (amount > max) {
            plugin.messages().send(sender(ctx), "errors.amount-too-high", Placeholder.unparsed("max", String.valueOf(max)));
            return false;
        }
        return true;
    }

    static CommandSender sender(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getSender();
    }

    Optional<Player> player(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getExecutor() instanceof Player player) {
            return Optional.of(player);
        }
        if (sender(ctx) instanceof Player player) {
            return Optional.of(player);
        }
        plugin.messages().send(sender(ctx), "errors.player-only");
        return Optional.empty();
    }

    TagResolver currencyTag(CurrencyDefinition def) {
        return Placeholder.component("currency", coins().displayName(def));
    }

    Component coinName(CurrencyDefinition def) {
        return coins().displayName(def);
    }

    /** "5 Moneda de Oro, 3 Moneda de Plata" */
    Component summary(Map<CurrencyDefinition, ? extends Number> amounts) {
        if (amounts.isEmpty()) {
            return Component.text("-", NamedTextColor.GRAY);
        }
        return Component.join(
                JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)),
                amounts.entrySet().stream()
                        .map(e -> Component.text(e.getValue().longValue() + " ", NamedTextColor.WHITE).append(coinName(e.getKey())))
                        .toList()
        );
    }

    /** Espacio disponible para recibir los stacks indicados (aproximado y conservador). */
    boolean canFit(Player player, Map<CurrencyDefinition, Long> incoming) {
        int maxStack = coins().maxStackSize();
        ItemStack[] storage = player.getInventory().getStorageContents();
        int emptySlots = 0;
        for (ItemStack item : storage) {
            if (item == null || item.isEmpty()) {
                emptySlots++;
            }
        }
        long neededSlots = 0;
        for (Map.Entry<CurrencyDefinition, Long> entry : incoming.entrySet()) {
            long free = 0;
            for (ItemStack item : storage) {
                if (item != null && coins().identify(item).filter(entry.getKey()::equals).isPresent()) {
                    free += Math.max(0, maxStack - item.getAmount());
                }
            }
            long rest = Math.max(0, entry.getValue() - free);
            neededSlots += (rest + maxStack - 1) / maxStack;
        }
        return neededSlots <= emptySlots;
    }

    void log(String message) {
        if (plugin.settings().logTransactions()) {
            plugin.getLogger().info(message);
        }
    }

    static String targetLabel(java.util.List<Player> targets) {
        return targets.size() == 1 ? targets.getFirst().getName() : targets.size() + " jugadores";
    }
}

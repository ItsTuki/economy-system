package cl.auryx.economy.command;

import cl.auryx.economy.EconomyPlugin;
import cl.auryx.economy.currency.CurrencyDefinition;
import cl.auryx.economy.currency.CurrencyManager;
import cl.auryx.economy.util.Credits;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /coins — comandos para jugadores: saldo, cambio, agrupar y pagar. */
public final class PlayerCoinsCommand {
    private final EconomyPlugin plugin;
    private final CommandSupport support;

    public PlayerCoinsCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.support = new CommandSupport(plugin);
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("coins")
                .requires(src -> src.getSender().hasPermission("economy.coins.balance"))
                .executes(this::balance)
                .then(Commands.literal("help").executes(ctx -> {
                    plugin.messages().sendList(CommandSupport.sender(ctx), "help.player");
                    CommandSupport.sender(ctx).sendMessage(Credits.helpLine());
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("balance").executes(this::balance))
                .then(Commands.literal("exchange")
                        .requires(src -> src.getSender().hasPermission("economy.coins.exchange"))
                        .then(Commands.argument("de", StringArgumentType.word())
                                .suggests(support.currencySuggestions())
                                .then(Commands.argument("a", StringArgumentType.word())
                                        .suggests(support.currencySuggestions())
                                        .then(Commands.literal("all").executes(ctx -> exchange(ctx, true)))
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                                .executes(ctx -> exchange(ctx, false))))))
                .then(Commands.literal("condense")
                        .requires(src -> src.getSender().hasPermission("economy.coins.condense"))
                        .executes(this::condense))
                .then(Commands.literal("pay")
                        .requires(src -> src.getSender().hasPermission("economy.coins.pay"))
                        .then(Commands.argument("jugador", ArgumentTypes.player())
                                .then(Commands.argument("moneda", StringArgumentType.word())
                                        .suggests(support.currencySuggestions())
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                                .executes(this::pay)))))
                .build();
    }

    // ── Saldo ────────────────────────────────────────────────

    private int balance(CommandContext<CommandSourceStack> ctx) {
        Optional<Player> player = support.player(ctx);
        if (player.isEmpty()) {
            return 0;
        }
        sendBalance(plugin, support, player.get(), player.get(), true);
        return Command.SINGLE_SUCCESS;
    }

    static void sendBalance(EconomyPlugin plugin, CommandSupport support, CommandSender viewer, Player target, boolean self) {
        CurrencyManager coins = support.coins();
        Map<CurrencyDefinition, Integer> counts = coins.countAll(target.getInventory());
        if (counts.isEmpty()) {
            plugin.messages().send(viewer, "balance.empty");
            return;
        }
        if (self) {
            plugin.messages().send(viewer, "balance.header-self");
        } else {
            plugin.messages().send(viewer, "balance.header-other", Placeholder.unparsed("target", target.getName()));
        }
        counts.forEach((def, amount) -> plugin.messages().send(viewer, "balance.entry",
                support.currencyTag(def),
                Placeholder.unparsed("amount", String.valueOf(amount)),
                Placeholder.unparsed("value", String.valueOf(def.value() * amount))));
        Component base = coins.baseCurrency().map(support::coinName).orElse(Component.text("unidades"));
        plugin.messages().send(viewer, "balance.total",
                Placeholder.unparsed("value", String.valueOf(CurrencyManager.totalValue(counts))),
                Placeholder.component("base", base));
    }

    // ── Cambio ───────────────────────────────────────────────

    private int exchange(CommandContext<CommandSourceStack> ctx, boolean all) {
        if (!plugin.settings().exchangeEnabled()) {
            plugin.messages().send(CommandSupport.sender(ctx), "errors.feature-disabled");
            return 0;
        }
        Optional<Player> playerOpt = support.player(ctx);
        Optional<CurrencyDefinition> fromOpt = support.currency(ctx, "de");
        if (playerOpt.isEmpty() || fromOpt.isEmpty()) {
            return 0;
        }
        Optional<CurrencyDefinition> toOpt = support.currency(ctx, "a");
        if (toOpt.isEmpty()) {
            return 0;
        }
        Player player = playerOpt.get();
        CurrencyDefinition from = fromOpt.get();
        CurrencyDefinition to = toOpt.get();
        if (from.equals(to)) {
            plugin.messages().send(player, "exchange.same");
            return 0;
        }

        CurrencyManager coins = support.coins();
        int owned = coins.count(player.getInventory(), from);
        int requested = all ? owned : IntegerArgumentType.getInteger(ctx, "cantidad");
        if (requested <= 0 || owned < requested) {
            plugin.messages().send(player, "errors.not-enough",
                    Placeholder.unparsed("amount", String.valueOf(Math.max(1, requested))), support.currencyTag(from));
            return 0;
        }

        long toAmount = (long) requested * from.value() / to.value();
        if (toAmount <= 0) {
            plugin.messages().send(player, "exchange.too-small",
                    Placeholder.unparsed("amount", String.valueOf(requested)),
                    Placeholder.component("from", support.coinName(from)),
                    Placeholder.component("to", support.coinName(to)));
            return 0;
        }
        if (!support.checkAmount(ctx, toAmount)) {
            return 0;
        }
        // Solo se consume lo necesario; el resto se devuelve como vuelto.
        long consumedValue = toAmount * to.value();
        int used = (int) Math.ceilDiv(consumedValue, from.value());
        long leftoverValue = used * from.value() - consumedValue;
        CurrencyManager.Breakdown change = coins.breakdown(leftoverValue);
        if (!change.exact()) {
            plugin.messages().send(player, "exchange.inexact");
            return 0;
        }

        Map<CurrencyDefinition, Long> incoming = new LinkedHashMap<>();
        incoming.put(to, toAmount);
        change.coins().forEach((def, n) -> incoming.merge(def, n, Long::sum));
        if (!support.canFit(player, incoming)) {
            plugin.messages().send(player, "errors.inventory-full");
            return 0;
        }

        coins.remove(player.getInventory(), from, used);
        incoming.forEach((def, n) -> coins.give(player, def, n.intValue(), true));

        plugin.messages().send(player, "exchange.success",
                Placeholder.unparsed("from-amount", String.valueOf(used)),
                Placeholder.component("from", support.coinName(from)),
                Placeholder.unparsed("to-amount", String.valueOf(toAmount)),
                Placeholder.component("to", support.coinName(to)));
        if (!change.coins().isEmpty()) {
            plugin.messages().send(player, "exchange.change", Placeholder.component("change", support.summary(change.coins())));
        }
        return Command.SINGLE_SUCCESS;
    }

    // ── Agrupar ──────────────────────────────────────────────

    private int condense(CommandContext<CommandSourceStack> ctx) {
        if (!plugin.settings().condenseEnabled()) {
            plugin.messages().send(CommandSupport.sender(ctx), "errors.feature-disabled");
            return 0;
        }
        Optional<Player> playerOpt = support.player(ctx);
        if (playerOpt.isEmpty()) {
            return 0;
        }
        Player player = playerOpt.get();
        CurrencyManager coins = support.coins();
        Map<CurrencyDefinition, Integer> current = coins.countAll(player.getInventory());
        CurrencyManager.Breakdown target = coins.breakdown(CurrencyManager.totalValue(current));
        if (!target.exact()) {
            plugin.messages().send(player, "exchange.inexact");
            return 0;
        }
        Map<CurrencyDefinition, Long> currentLong = new LinkedHashMap<>();
        current.forEach((def, n) -> currentLong.put(def, n.longValue()));
        if (currentLong.equals(target.coins())) {
            plugin.messages().send(player, "condense.nothing");
            return 0;
        }
        current.keySet().forEach(def -> coins.removeAll(player.getInventory(), def));
        target.coins().forEach((def, n) -> coins.give(player, def, (int) Math.min(Integer.MAX_VALUE, n), true));
        plugin.messages().send(player, "condense.success", Placeholder.component("summary", support.summary(target.coins())));
        return Command.SINGLE_SUCCESS;
    }

    // ── Pagar ────────────────────────────────────────────────

    private int pay(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!plugin.settings().payEnabled()) {
            plugin.messages().send(CommandSupport.sender(ctx), "errors.feature-disabled");
            return 0;
        }
        Optional<Player> senderOpt = support.player(ctx);
        if (senderOpt.isEmpty()) {
            return 0;
        }
        Player sender = senderOpt.get();
        List<Player> resolved = ctx.getArgument("jugador", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
        if (resolved.isEmpty()) {
            plugin.messages().send(sender, "errors.no-targets");
            return 0;
        }
        Player target = resolved.getFirst();
        if (target.equals(sender)) {
            plugin.messages().send(sender, "pay.self");
            return 0;
        }
        double maxDistance = plugin.settings().payMaxDistance();
        if (maxDistance > 0 && (!target.getWorld().equals(sender.getWorld())
                || target.getLocation().distanceSquared(sender.getLocation()) > maxDistance * maxDistance)) {
            plugin.messages().send(sender, "pay.too-far", Placeholder.unparsed("max", String.valueOf((int) maxDistance)));
            return 0;
        }
        Optional<CurrencyDefinition> def = support.currency(ctx, "moneda");
        int amount = IntegerArgumentType.getInteger(ctx, "cantidad");
        if (def.isEmpty() || !support.checkAmount(ctx, amount)) {
            return 0;
        }
        CurrencyManager coins = support.coins();
        if (coins.count(sender.getInventory(), def.get()) < amount) {
            plugin.messages().send(sender, "errors.not-enough",
                    Placeholder.unparsed("amount", String.valueOf(amount)), support.currencyTag(def.get()));
            return 0;
        }
        coins.remove(sender.getInventory(), def.get(), amount);
        coins.give(target, def.get(), amount, true);
        plugin.messages().send(sender, "pay.sender",
                Placeholder.unparsed("amount", String.valueOf(amount)), support.currencyTag(def.get()),
                Placeholder.unparsed("target", target.getName()));
        plugin.messages().send(target, "pay.target",
                Placeholder.unparsed("amount", String.valueOf(amount)), support.currencyTag(def.get()),
                Placeholder.unparsed("player", sender.getName()));
        support.log(sender.getName() + " pagó " + amount + " " + def.get().id() + " a " + target.getName());
        return Command.SINGLE_SUCCESS;
    }
}

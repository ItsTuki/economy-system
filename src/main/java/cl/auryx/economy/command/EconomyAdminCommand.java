package cl.auryx.economy.command;

import cl.auryx.economy.EconomyPlugin;
import cl.auryx.economy.currency.CurrencyDefinition;
import cl.auryx.economy.currency.CurrencyConfigLoader;
import cl.auryx.economy.currency.CurrencyManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/** /economy — comandos de administración. */
public final class EconomyAdminCommand {
    private static final String PERM = "economy.admin.";
    private static final List<String> COLOR_SUGGESTIONS = List.of("gold", "yellow", "aqua", "green", "red", "light_purple", "#FFAA00", "#55FFFF");
    private static final List<String> MATERIAL_SUGGESTIONS = List.of(
            "GOLD_NUGGET", "IRON_NUGGET", "COPPER_NUGGET", "EMERALD", "DIAMOND", "AMETHYST_SHARD",
            "NETHER_STAR", "PRISMARINE_CRYSTALS", "ECHO_SHARD", "HEART_OF_THE_SEA", "PAPER");

    private final EconomyPlugin plugin;
    private final CommandSupport support;

    public EconomyAdminCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.support = new CommandSupport(plugin);
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("economy")
                .requires(src -> src.getSender().hasPermission("economy.admin"))
                .executes(this::help)
                .then(Commands.literal("help").executes(this::help))
                .then(Commands.literal("list")
                        .requires(perm("list"))
                        .executes(this::list))
                .then(Commands.literal("give")
                        .requires(perm("give"))
                        .then(Commands.argument("jugadores", ArgumentTypes.players())
                                .then(Commands.argument("moneda", StringArgumentType.word())
                                        .suggests(support.currencySuggestions())
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                                .suggests((ctx, b) -> {
                                                    List.of("1", "16", "64", "99").forEach(b::suggest);
                                                    return b.buildFuture();
                                                })
                                                .executes(this::give)))))
                .then(Commands.literal("take")
                        .requires(perm("take"))
                        .then(Commands.argument("jugadores", ArgumentTypes.players())
                                .then(Commands.argument("moneda", StringArgumentType.word())
                                        .suggests(support.currencySuggestions())
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                                .executes(this::take)))))
                .then(Commands.literal("remove")
                        .requires(perm("remove"))
                        .then(Commands.argument("jugadores", ArgumentTypes.players())
                                .executes(ctx -> remove(ctx, false))
                                .then(Commands.argument("moneda", StringArgumentType.word())
                                        .suggests(support.currencySuggestions())
                                        .executes(ctx -> remove(ctx, true)))))
                .then(Commands.literal("balance")
                        .requires(perm("balance"))
                        .then(Commands.argument("jugador", ArgumentTypes.player())
                                .executes(this::balance)))
                .then(Commands.literal("add")
                        .requires(perm("add"))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("material", StringArgumentType.word())
                                        .suggests((ctx, b) -> {
                                            String rem = b.getRemaining().toUpperCase(Locale.ROOT);
                                            if (rem.isEmpty()) {
                                                MATERIAL_SUGGESTIONS.forEach(b::suggest);
                                            } else {
                                                int added = 0;
                                                for (Material m : Material.values()) {
                                                    if (added >= 40) {
                                                        break;
                                                    }
                                                    if (m.isItem() && !m.isAir() && !m.isLegacy() && m.name().startsWith(rem)) {
                                                        b.suggest(m.name());
                                                        added++;
                                                    }
                                                }
                                            }
                                            return b.buildFuture();
                                        })
                                        .then(Commands.argument("color", StringArgumentType.string())
                                                .suggests((ctx, b) -> {
                                                    COLOR_SUGGESTIONS.forEach(c -> b.suggest(c.startsWith("#") ? "\"" + c + "\"" : c));
                                                    return b.buildFuture();
                                                })
                                                .then(Commands.argument("valor", LongArgumentType.longArg(1))
                                                        .then(Commands.argument("nombre", StringArgumentType.greedyString())
                                                                .executes(this::add)))))))
                .then(Commands.literal("info")
                        .requires(perm("info"))
                        .executes(this::info))
                .then(Commands.literal("reload")
                        .requires(perm("reload"))
                        .executes(this::reload))
                .build();
    }

    private static java.util.function.Predicate<CommandSourceStack> perm(String node) {
        return src -> src.getSender().hasPermission(PERM + node);
    }

    // ── Subcomandos ──────────────────────────────────────────

    private int help(CommandContext<CommandSourceStack> ctx) {
        plugin.messages().sendList(CommandSupport.sender(ctx), "help.admin",
                Placeholder.unparsed("version", plugin.getPluginMeta().getVersion()));
        return Command.SINGLE_SUCCESS;
    }

    private int list(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = CommandSupport.sender(ctx);
        plugin.messages().send(sender, "list.header",
                Placeholder.unparsed("count", String.valueOf(support.coins().definitions().size())));
        for (CurrencyDefinition def : support.coins().byValueDescending()) {
            plugin.messages().send(sender, "list.entry",
                    support.currencyTag(def),
                    Placeholder.unparsed("id", def.id()),
                    Placeholder.unparsed("value", String.valueOf(def.value())),
                    Placeholder.unparsed("material", def.material().name()));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int give(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<Player> targets = targets(ctx);
        Optional<CurrencyDefinition> def = support.currency(ctx, "moneda");
        int amount = IntegerArgumentType.getInteger(ctx, "cantidad");
        if (targets.isEmpty() || def.isEmpty() || !support.checkAmount(ctx, amount)) {
            return 0;
        }
        CommandSender sender = CommandSupport.sender(ctx);
        boolean drop = plugin.settings().dropWhenFull();
        for (Player target : targets) {
            int given = support.coins().give(target, def.get(), amount, drop);
            if (given < amount) {
                plugin.messages().send(sender, "give.partial",
                        Placeholder.unparsed("target", target.getName()),
                        Placeholder.unparsed("given", String.valueOf(given)),
                        Placeholder.unparsed("amount", String.valueOf(amount)));
            }
            if (given > 0 && plugin.settings().notifyTargets() && target != sender) {
                plugin.messages().send(target, "give.target",
                        Placeholder.unparsed("amount", String.valueOf(given)), support.currencyTag(def.get()));
            }
        }
        plugin.messages().send(sender, "give.sender",
                Placeholder.unparsed("amount", String.valueOf(amount)),
                support.currencyTag(def.get()),
                Placeholder.unparsed("target", CommandSupport.targetLabel(targets)),
                Placeholder.unparsed("count", String.valueOf(targets.size())));
        support.log(sender.getName() + " entregó " + amount + " " + def.get().id() + " a " + names(targets));
        return targets.size();
    }

    private int take(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<Player> targets = targets(ctx);
        Optional<CurrencyDefinition> def = support.currency(ctx, "moneda");
        int amount = IntegerArgumentType.getInteger(ctx, "cantidad");
        if (targets.isEmpty() || def.isEmpty() || !support.checkAmount(ctx, amount)) {
            return 0;
        }
        CommandSender sender = CommandSupport.sender(ctx);
        int total = 0;
        for (Player target : targets) {
            int taken = support.coins().remove(target.getInventory(), def.get(), amount);
            total += taken;
            if (taken > 0 && plugin.settings().notifyTargets() && target != sender) {
                plugin.messages().send(target, "take.target",
                        Placeholder.unparsed("amount", String.valueOf(taken)), support.currencyTag(def.get()));
            }
        }
        plugin.messages().send(sender, "take.sender",
                Placeholder.unparsed("amount", String.valueOf(total)),
                Placeholder.unparsed("requested", String.valueOf(amount)),
                support.currencyTag(def.get()),
                Placeholder.unparsed("target", CommandSupport.targetLabel(targets)));
        support.log(sender.getName() + " retiró " + total + " " + def.get().id() + " de " + names(targets));
        return total;
    }

    private int remove(CommandContext<CommandSourceStack> ctx, boolean withCurrency) throws CommandSyntaxException {
        List<Player> targets = targets(ctx);
        CurrencyDefinition filter = null;
        if (withCurrency) {
            Optional<CurrencyDefinition> def = support.currency(ctx, "moneda");
            if (def.isEmpty()) {
                return 0;
            }
            filter = def.get();
        }
        if (targets.isEmpty()) {
            return 0;
        }
        CommandSender sender = CommandSupport.sender(ctx);
        int total = 0;
        for (Player target : targets) {
            int removed = support.coins().removeAll(target.getInventory(), filter);
            total += removed;
            if (removed > 0 && plugin.settings().notifyTargets() && target != sender) {
                plugin.messages().send(target, "remove.target");
            }
        }
        plugin.messages().send(sender, "remove.sender",
                Placeholder.unparsed("amount", String.valueOf(total)),
                Placeholder.unparsed("target", CommandSupport.targetLabel(targets)));
        support.log(sender.getName() + " eliminó " + total + " monedas" + (filter != null ? " (" + filter.id() + ")" : "") + " de " + names(targets));
        return total;
    }

    private int balance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<Player> resolved = ctx.getArgument("jugador", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
        if (resolved.isEmpty()) {
            plugin.messages().send(CommandSupport.sender(ctx), "errors.no-targets");
            return 0;
        }
        PlayerCoinsCommand.sendBalance(plugin, support, CommandSupport.sender(ctx), resolved.getFirst(), false);
        return Command.SINGLE_SUCCESS;
    }

    private int add(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = CommandSupport.sender(ctx);
        String id = ensureCoinSuffix(CurrencyManager.normalize(StringArgumentType.getString(ctx, "id")));
        String base = stripCoinSuffix(id);
        if (support.coins().exists(id) || (!base.isEmpty() && support.coins().exists(base))) {
            plugin.messages().send(sender, "add.exists");
            return 0;
        }
        String materialInput = StringArgumentType.getString(ctx, "material");
        Material material = Material.matchMaterial(materialInput);
        if (material == null || !material.isItem() || material.isAir()) {
            plugin.messages().send(sender, "add.invalid-material", Placeholder.unparsed("input", materialInput));
            return 0;
        }
        @Nullable TextColor color = CurrencyConfigLoader.parseColor(StringArgumentType.getString(ctx, "color"));
        if (color == null) {
            plugin.messages().send(sender, "add.invalid-color");
            return 0;
        }
        long value = LongArgumentType.getLong(ctx, "valor");
        String name = StringArgumentType.getString(ctx, "nombre");
        List<String> aliases = base.isEmpty() || base.equals(id) ? List.of() : List.of(base);
        CurrencyDefinition def = new CurrencyDefinition(id, name, name, color, material,
                List.of("&7Divisa creada por " + sender.getName()), aliases, value, null, false);
        if (!plugin.addCurrency(def)) {
            plugin.messages().send(sender, "add.failed");
            return 0;
        }
        plugin.messages().send(sender, "add.success", support.currencyTag(def), Placeholder.unparsed("id", id));
        support.log(sender.getName() + " creó la moneda " + id + " (" + material + ", valor " + value + ")");
        return Command.SINGLE_SUCCESS;
    }

    private int info(CommandContext<CommandSourceStack> ctx) {
        Optional<Player> player = support.player(ctx);
        if (player.isEmpty()) {
            return 0;
        }
        var hand = player.get().getInventory().getItemInMainHand();
        String storedId = support.coins().storedId(hand);
        if (storedId == null) {
            plugin.messages().send(player.get(), "info.not-coin");
            return 0;
        }
        Optional<CurrencyDefinition> def = support.coins().identify(hand);
        plugin.messages().send(CommandSupport.sender(ctx), "info.coin",
                def.map(support::currencyTag).orElse(Placeholder.unparsed("currency", storedId + " (no registrada)")),
                Placeholder.unparsed("id", storedId),
                Placeholder.unparsed("value", def.map(d -> String.valueOf(d.value())).orElse("?")),
                Placeholder.unparsed("amount", String.valueOf(hand.getAmount())));
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandContext<CommandSourceStack> ctx) {
        plugin.reloadAll();
        plugin.messages().send(CommandSupport.sender(ctx), "reload.success",
                Placeholder.unparsed("count", String.valueOf(support.coins().definitions().size())));
        return Command.SINGLE_SUCCESS;
    }

    // ── Helpers ──────────────────────────────────────────────

    private List<Player> targets(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<Player> players = ctx.getArgument("jugadores", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());
        if (players.isEmpty()) {
            plugin.messages().send(CommandSupport.sender(ctx), "errors.no-targets");
        }
        return players;
    }

    private static String names(List<Player> players) {
        List<String> names = new ArrayList<>(players.size());
        players.forEach(p -> names.add(p.getName()));
        return String.join(", ", names);
    }

    static String ensureCoinSuffix(String value) {
        String base = stripCoinSuffix(value);
        return (base.isEmpty() ? "currency" : base) + CurrencyManager.COIN_SUFFIX;
    }

    static String stripCoinSuffix(String value) {
        String base = value.replace('_', '-');
        if (base.endsWith(CurrencyManager.COIN_SUFFIX)) {
            base = base.substring(0, base.length() - CurrencyManager.COIN_SUFFIX.length());
        }
        while (base.endsWith("-")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }
}

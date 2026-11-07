package cl.auryx.economy.currency;

import cl.auryx.economy.util.TextFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/** Registro de monedas y operaciones sobre ítems e inventarios. */
public final class CurrencyManager {
    /** Sufijo que el plugin agrega a los identificadores creados con /economy add. */
    public static final String COIN_SUFFIX = "-coin";

    private final NamespacedKey currencyKey;
    private final Map<String, CurrencyDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, CurrencyDefinition> lookup = new LinkedHashMap<>();
    private List<CurrencyDefinition> byValueDesc = List.of();
    private int maxStackSize = 99;

    public CurrencyManager(Plugin plugin) {
        // La clave se mantiene igual que en v1 (economyitems:currency-id) para que las monedas existentes sigan siendo válidas.
        this.currencyKey = new NamespacedKey(plugin, "currency-id");
    }

    public void setDefinitions(Collection<CurrencyDefinition> newDefinitions) {
        definitions.clear();
        lookup.clear();
        for (CurrencyDefinition def : newDefinitions) {
            definitions.put(normalize(def.id()), def);
        }
        // Primero ids, luego alias: un alias nunca pisa el id de otra moneda.
        for (CurrencyDefinition def : definitions.values()) {
            lookup.put(normalize(def.id()), def);
        }
        for (CurrencyDefinition def : definitions.values()) {
            for (String alias : def.aliases()) {
                if (alias != null && !alias.isBlank()) {
                    lookup.putIfAbsent(normalize(alias), def);
                }
            }
        }
        byValueDesc = definitions.values().stream()
                .sorted(Comparator.comparingLong(CurrencyDefinition::value).reversed())
                .toList();
    }

    public void setMaxStackSize(int maxStackSize) {
        this.maxStackSize = Math.max(1, Math.min(99, maxStackSize));
    }

    public int maxStackSize() {
        return maxStackSize;
    }

    public Collection<CurrencyDefinition> definitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    /** Monedas ordenadas de mayor a menor valor. */
    public List<CurrencyDefinition> byValueDescending() {
        return byValueDesc;
    }

    /** Moneda de menor valor; si hay empate, la primera definida en currencies.yml. */
    public Optional<CurrencyDefinition> baseCurrency() {
        return definitions.values().stream().min(Comparator.comparingLong(CurrencyDefinition::value));
    }

    public Optional<CurrencyDefinition> find(@Nullable String idOrAlias) {
        if (idOrAlias == null || idOrAlias.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(lookup.get(normalize(idOrAlias)));
    }

    public boolean exists(String idOrAlias) {
        return find(idOrAlias).isPresent();
    }

    // ── Ítems ────────────────────────────────────────────────

    public ItemStack create(CurrencyDefinition def, int amount) {
        ItemStack stack = ItemStack.of(def.material());
        applyMeta(stack, def);
        stack.setAmount(Math.max(1, Math.min(maxStackSize, amount)));
        return stack;
    }

    public Component displayName(CurrencyDefinition def) {
        return TextFormat.item(def.formattedName(), def.color());
    }

    private void applyMeta(ItemStack stack, CurrencyDefinition def) {
        stack.editMeta(meta -> {
            meta.displayName(displayName(def));
            meta.lore(buildLore(def));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            meta.setMaxStackSize(maxStackSize);
            meta.setItemModel(def.itemModel());
            meta.setEnchantmentGlintOverride(def.glow() ? Boolean.TRUE : null);
            meta.getPersistentDataContainer().set(currencyKey, PersistentDataType.STRING, def.id());
        });
    }

    private List<Component> buildLore(CurrencyDefinition def) {
        List<String> lines = def.lore().isEmpty() ? List.of("&7Divisa personalizada") : def.lore();
        return lines.stream().map(line -> TextFormat.item(line, NamedTextColor.GRAY)).toList();
    }

    /** Id guardado en el ítem, o {@code null} si no es una moneda de este plugin. */
    public @Nullable String storedId(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.getPersistentDataContainer().get(currencyKey, PersistentDataType.STRING);
    }

    /** Cualquier ítem marcado por el plugin, aunque su moneda ya no exista en currencies.yml. */
    public boolean isCoin(@Nullable ItemStack stack) {
        return storedId(stack) != null;
    }

    public boolean containsCoin(@Nullable ItemStack @Nullable [] items) {
        if (items == null) {
            return false;
        }
        for (ItemStack item : items) {
            if (isCoin(item)) {
                return true;
            }
        }
        return false;
    }

    public Optional<CurrencyDefinition> identify(@Nullable ItemStack stack) {
        String id = storedId(stack);
        return id == null ? Optional.empty() : Optional.ofNullable(definitions.get(normalize(id)));
    }

    // ── Inventarios ──────────────────────────────────────────

    public int count(Inventory inventory, CurrencyDefinition def) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (matches(item, def)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /** Cantidad por moneda, de mayor a menor valor (solo monedas con cantidad > 0). */
    public Map<CurrencyDefinition, Integer> countAll(Inventory inventory) {
        Map<CurrencyDefinition, Integer> result = new LinkedHashMap<>();
        for (CurrencyDefinition def : byValueDesc) {
            int amount = count(inventory, def);
            if (amount > 0) {
                result.put(def, amount);
            }
        }
        return result;
    }

    public static long totalValue(Map<CurrencyDefinition, ? extends Number> amounts) {
        long total = 0;
        for (Map.Entry<CurrencyDefinition, ? extends Number> entry : amounts.entrySet()) {
            total += entry.getKey().value() * entry.getValue().longValue();
        }
        return total;
    }

    /** Quita hasta {@code amount} monedas. Devuelve cuántas se quitaron. */
    public int remove(Inventory inventory, CurrencyDefinition def, int amount) {
        return removeMatching(inventory, item -> matches(item, def), amount);
    }

    /** Quita todas las monedas (o solo las de {@code def} si no es null). */
    public int removeAll(Inventory inventory, @Nullable CurrencyDefinition def) {
        Predicate<ItemStack> filter = def == null ? this::isCoin : item -> matches(item, def);
        return removeMatching(inventory, filter, Integer.MAX_VALUE);
    }

    private int removeMatching(Inventory inventory, Predicate<ItemStack> filter, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (item == null || !filter.test(item)) {
                continue;
            }
            int take = Math.min(remaining, item.getAmount());
            remaining -= take;
            if (take >= item.getAmount()) {
                inventory.clear(slot);
            } else {
                item.setAmount(item.getAmount() - take);
                inventory.setItem(slot, item);
            }
        }
        return amount == Integer.MAX_VALUE ? Integer.MAX_VALUE - remaining : amount - remaining;
    }

    /**
     * Entrega monedas a un jugador en stacks de {@link #maxStackSize()}.
     *
     * @param dropOverflow si es true, lo que no cabe cae a los pies del jugador
     * @return cantidad efectivamente entregada (incluye lo tirado al suelo)
     */
    public int give(Player player, CurrencyDefinition def, int amount, boolean dropOverflow) {
        int remaining = amount;
        int delivered = 0;
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStackSize);
            remaining -= stackAmount;
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(create(def, stackAmount));
            int notAdded = overflow.values().stream().mapToInt(ItemStack::getAmount).sum();
            delivered += stackAmount - notAdded;
            if (notAdded > 0) {
                if (dropOverflow) {
                    overflow.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
                    delivered += notAdded;
                } else {
                    break;
                }
            }
        }
        return delivered;
    }

    /** Vuelve a aplicar nombre, lore y modelo a las monedas conocidas del inventario. */
    public int refresh(Inventory inventory) {
        int updated = 0;
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            Optional<CurrencyDefinition> def = identify(item);
            if (def.isEmpty()) {
                continue;
            }
            ItemStack fresh = ItemStack.of(def.get().material());
            applyMeta(fresh, def.get());
            fresh.setAmount(Math.min(item.getAmount(), maxStackSize));
            if (!fresh.isSimilar(item)) {
                inventory.setItem(slot, fresh);
                updated++;
            }
        }
        return updated;
    }

    /**
     * Descompone un valor en la menor cantidad de monedas (algoritmo voraz).
     * Si sobra un resto que no se puede representar, queda en {@link Breakdown#remainder()}.
     */
    public Breakdown breakdown(long value) {
        Map<CurrencyDefinition, Long> coins = new LinkedHashMap<>();
        long remaining = value;
        for (CurrencyDefinition def : byValueDesc) {
            if (remaining <= 0) {
                break;
            }
            long count = remaining / def.value();
            if (count > 0) {
                coins.put(def, count);
                remaining -= count * def.value();
            }
        }
        return new Breakdown(coins, remaining);
    }

    public record Breakdown(Map<CurrencyDefinition, Long> coins, long remainder) {
        public boolean exact() {
            return remainder == 0;
        }
    }

    private boolean matches(@Nullable ItemStack item, CurrencyDefinition def) {
        String id = storedId(item);
        return id != null && normalize(id).equals(normalize(def.id()));
    }

    public List<String> suggestionIds() {
        List<String> ids = new ArrayList<>();
        definitions.values().forEach(def -> ids.add(def.id()));
        return ids;
    }

    public static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim().replace(' ', '-').replace('_', '-');
    }
}

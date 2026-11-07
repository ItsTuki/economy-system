package cl.auryx.economy.currency;

import com.mc.economy.EconomyPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class CurrencyManager {
   public static final int MAX_STACK_SIZE = 99;
   private static final String COIN_SUFFIX = "-coin";
   private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder().character('&').hexColors().build();
   private final NamespacedKey currencyKey;
   private final Map<String, CurrencyDefinition> definitions = new LinkedHashMap<>();
   private final Map<String, CurrencyDefinition> lookup = new LinkedHashMap<>();

   public CurrencyManager(EconomyPlugin plugin) {
      this.currencyKey = new NamespacedKey(plugin, "currency-id");
   }

   public void setDefinitions(Collection<CurrencyDefinition> newDefinitions) {
      this.definitions.clear();
      this.lookup.clear();

      for (CurrencyDefinition definition : newDefinitions) {
         this.definitions.put(definition.id(), definition);
         this.registerLookup(definition.id(), definition);
         List<String> aliases = definition.aliases();
         if (aliases != null) {
            aliases.forEach(alias -> this.registerLookup(alias, definition));
         }
      }
   }

   private void registerLookup(String key, CurrencyDefinition definition) {
      if (key != null && !key.isEmpty()) {
         this.lookup.put(normalize(key), definition);
      }
   }

   public Collection<CurrencyDefinition> getDefinitions() {
      return Collections.unmodifiableCollection(this.definitions.values());
   }

   public Optional<CurrencyDefinition> findCurrency(String input) {
      return input != null && !input.isEmpty() ? Optional.ofNullable(this.lookup.get(normalize(input))) : Optional.empty();
   }

   public boolean exists(String idOrAlias) {
      return this.findCurrency(idOrAlias).isPresent();
   }

   public ItemStack createCurrency(CurrencyDefinition definition, int amount) {
      ItemStack stack = new ItemStack(definition.material());
      stack.setAmount(Math.max(1, Math.min(99, amount)));
      ItemMeta meta = stack.getItemMeta();
      if (meta != null) {
         String styled = definition.styledDisplayName() != null ? definition.styledDisplayName() : definition.displayName();
         meta.displayName(this.formatText(styled, definition.color()));
         meta.lore(this.buildLore(definition));
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS});
         meta.setMaxStackSize(99);
         PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
         dataContainer.set(this.currencyKey, PersistentDataType.STRING, definition.id());
         stack.setItemMeta(meta);
      }

      return stack;
   }

   public boolean isCurrency(ItemStack stack) {
      return this.isCoinStack(stack);
   }

   public Optional<CurrencyDefinition> identifyCurrency(ItemStack stack) {
      String storedId = this.extractCurrencyId(stack);
      return storedId == null ? Optional.empty() : Optional.ofNullable(this.lookup.get(normalize(storedId)));
   }

   public boolean containsCurrency(ItemStack[] items) {
      return items == null ? false : Arrays.stream(items).anyMatch(this::isCoinStack);
   }

   public boolean isCoinStack(ItemStack stack) {
      String storedId = this.extractCurrencyId(stack);
      return storedId != null && this.isCoinId(storedId);
   }

   public boolean isCoinId(String value) {
      return value != null && normalize(value).endsWith("-coin");
   }

   private String extractCurrencyId(ItemStack stack) {
      if (stack != null && !stack.getType().isAir()) {
         ItemMeta meta = stack.getItemMeta();
         if (meta == null) {
            return null;
         } else {
            PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
            return (String)dataContainer.get(this.currencyKey, PersistentDataType.STRING);
         }
      } else {
         return null;
      }
   }

   private List<Component> buildLore(CurrencyDefinition definition) {
      List<String> loreLines = definition.lore();
      if (loreLines == null || loreLines.isEmpty()) {
         loreLines = new ArrayList<>();
         loreLines.add("&7Divisa personalizada");
      }

      return loreLines.stream().map(line -> this.formatText(line, NamedTextColor.GRAY)).collect(Collectors.toList());
   }

   private Component formatText(String text, TextColor fallbackColor) {
      if (text != null && !text.isEmpty()) {
         Component component = LEGACY_SERIALIZER.deserialize(text);
         return fallbackColor != null ? component.colorIfAbsent(fallbackColor) : component;
      } else {
         return Component.empty();
      }
   }

   private static String normalize(String value) {
      return value.toLowerCase(Locale.ROOT).replace(' ', '-').replace('_', '-');
   }
}

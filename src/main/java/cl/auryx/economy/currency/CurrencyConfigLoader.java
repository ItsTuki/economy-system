package cl.auryx.economy.currency;

import com.mc.economy.EconomyPlugin;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class CurrencyConfigLoader {
   private static final String FILE_NAME = "currencies.yml";

   public List<CurrencyDefinition> load(EconomyPlugin plugin) {
      File configFile = ensureConfigFile(plugin);
      YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
      ConfigurationSection root = config.getConfigurationSection("currencies");
      if (root == null) {
         root = config.createSection("currencies");
      }

      boolean updated = ensureDefaults(root);
      ConfigurationSection section = root;
      Logger logger = plugin.getLogger();
      List<CurrencyDefinition> definitions = section.getKeys(false)
         .stream()
         .map(key -> readCurrency(section.getConfigurationSection(key), key, logger))
         .toList();
      if (updated) {
         save(config, configFile, plugin.getLogger());
      }

      return definitions;
   }

   public boolean addCurrency(EconomyPlugin plugin, CurrencyDefinition definition) {
      File configFile = ensureConfigFile(plugin);
      YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
      ConfigurationSection root = config.getConfigurationSection("currencies");
      if (root == null) {
         root = config.createSection("currencies");
      }

      if (root.isConfigurationSection(definition.id())) {
         return false;
      } else {
         ConfigurationSection section = root.createSection(definition.id());
         writeCurrency(section, definition);
         save(config, configFile, plugin.getLogger());
         return true;
      }
   }

   private static CurrencyDefinition readCurrency(ConfigurationSection section, String key, Logger logger) {
      if (section == null) {
         return fallbackDefinition(key);
      } else {
         CurrencyDefinition defaults = CurrencyDefaults.defaults()
            .stream()
            .filter(def -> def.id().equalsIgnoreCase(key))
            .findFirst()
            .orElse(fallbackDefinition(key));
         String id = section.getString("id", defaults.id());
         String displayName = section.getString("display-name", defaults.displayName());
         String styledName = section.getString("styled-display-name", defaults.styledDisplayName());
         TextColor color = parseColor(section.getString("color"), defaults.color(), logger);
         Material material = parseMaterial(section.getString("material"), defaults.material(), logger);
         List<String> lore = section.getStringList("lore");
         if (lore == null || lore.isEmpty()) {
            lore = defaults.lore();
         }

         List<String> aliases = section.getStringList("aliases");
         if (aliases == null || aliases.isEmpty()) {
            aliases = defaults.aliases();
         }

         return new CurrencyDefinition(id, displayName, styledName, color, material, lore, aliases);
      }
   }

   private static void writeCurrency(ConfigurationSection section, CurrencyDefinition properties) {
      section.set("id", properties.id());
      section.set("display-name", properties.displayName());
      section.set("styled-display-name", properties.styledDisplayName());
      section.set("color", colorToConfigValue(properties.color()));
      section.set("material", properties.material().name());
      section.set("lore", properties.lore());
      section.set("aliases", properties.aliases());
   }

   private static boolean ensureDefaults(ConfigurationSection root) {
      boolean updated = false;

      for (CurrencyDefinition defaults : CurrencyDefaults.defaults()) {
         if (!root.isConfigurationSection(defaults.id())) {
            writeCurrency(root.createSection(defaults.id()), defaults);
            updated = true;
         }
      }

      return updated;
   }

   private static TextColor parseColor(String value, TextColor fallback, Logger logger) {
      if (value != null && !value.isEmpty()) {
         NamedTextColor named = (NamedTextColor)NamedTextColor.NAMES.value(value.toLowerCase(Locale.ROOT));
         if (named != null) {
            return named;
         } else {
            try {
               TextColor parsed = TextColor.fromCSSHexString(value);
               if (parsed != null) {
                  return parsed;
               }
            } catch (IllegalArgumentException var5) {
            }

            logger.warning("Unknown currency color '" + value + "', using " + fallback + " instead");
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   private static Material parseMaterial(String value, Material fallback, Logger logger) {
      if (value != null && !value.isEmpty()) {
         Material material = Material.matchMaterial(value);
         if (material == null) {
            logger.warning("Unknown currency material '" + value + "', using " + fallback + " instead");
            return fallback;
         } else {
            return material;
         }
      } else {
         return fallback;
      }
   }

   private static String colorToConfigValue(TextColor color) {
      return color.asHexString();
   }

   private static void save(YamlConfiguration config, File file, Logger logger) {
      try {
         config.save(file);
      } catch (IOException var4) {
         logger.warning("Failed to save currencies.yml: " + var4.getMessage());
      }
   }

   private static File ensureConfigFile(EconomyPlugin plugin) {
      File dataFolder = plugin.getDataFolder();
      if (!dataFolder.exists() && !dataFolder.mkdirs()) {
         plugin.getLogger().warning("Unable to create plugin data folder; using in-memory currency defaults");
      }

      return new File(dataFolder, "currencies.yml");
   }

   private static CurrencyDefinition fallbackDefinition(String key) {
      return new CurrencyDefinition(key, key, key, NamedTextColor.WHITE, Material.PAPER, List.of("Custom currency"), List.of(key));
   }
}

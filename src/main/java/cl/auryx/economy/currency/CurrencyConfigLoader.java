package cl.auryx.economy.currency;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/** Lee y escribe {@code currencies.yml}. */
public final class CurrencyConfigLoader {
    public static final String FILE_NAME = "currencies.yml";
    private static final String ROOT = "currencies";

    private final Plugin plugin;

    public CurrencyConfigLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    public List<CurrencyDefinition> load() {
        File file = file();
        boolean firstRun = !file.exists();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = section(config);
        Logger logger = plugin.getLogger();

        boolean updated = false;
        if (firstRun) {
            config.options().setHeader(List.of(
                    "Monedas del servidor. Cada clave es una moneda.",
                    "  id                   Identificador guardado en el ítem. NO lo cambies si ya hay monedas en circulación.",
                    "  display-name         Nombre plano usado en mensajes.",
                    "  styled-display-name  Nombre del ítem. Acepta MiniMessage (<gold>...) o legacy (&#RRGGBB&l...).",
                    "  color                Color por defecto (nombre como 'gold' o hex '#FFAA00').",
                    "  material             Material del ítem (ej. GOLD_NUGGET).",
                    "  value                Valor en unidades base. Se usa para /coins exchange, /coins condense y el saldo.",
                    "  item-model           (opcional) Modelo de resource pack, ej. 'miservidor:moneda_oro'.",
                    "  glow                 (opcional) true para que brille como encantado.",
                    "  lore / aliases       Descripción y nombres alternativos para comandos."
            ));
            for (CurrencyDefinition def : CurrencyDefaults.defaults()) {
                write(root.createSection(def.id()), def);
            }
            updated = true;
        }

        List<CurrencyDefinition> definitions = new ArrayList<>();
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                logger.warning("Ignorando moneda '" + key + "': no es una sección válida");
                continue;
            }
            if (!section.contains("value")) {
                section.set("value", defaultValue(key));
                updated = true;
            }
            try {
                definitions.add(read(section, key, logger));
            } catch (IllegalArgumentException ex) {
                logger.warning("Ignorando moneda '" + key + "': " + ex.getMessage());
            }
        }

        if (updated) {
            save(config, file);
        }
        return definitions;
    }

    /** Agrega una moneda nueva. Devuelve {@code false} si el id ya existe. */
    public boolean add(CurrencyDefinition definition) {
        File file = file();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = section(config);
        if (root.isConfigurationSection(definition.id())) {
            return false;
        }
        write(root.createSection(definition.id()), definition);
        return save(config, file);
    }

    private static CurrencyDefinition read(ConfigurationSection section, String key, Logger logger) {
        CurrencyDefinition defaults = CurrencyDefaults.defaults().stream()
                .filter(def -> def.id().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);

        String id = section.getString("id", defaults != null ? defaults.id() : key);
        String displayName = section.getString("display-name", defaults != null ? defaults.displayName() : key);
        String styled = section.getString("styled-display-name", defaults != null ? defaults.styledDisplayName() : displayName);
        TextColor fallbackColor = defaults != null ? defaults.color() : NamedTextColor.WHITE;
        TextColor color = parseColor(section.getString("color"), fallbackColor, logger, key);
        Material fallbackMaterial = defaults != null ? defaults.material() : Material.PAPER;
        Material material = parseMaterial(section.getString("material"), fallbackMaterial, logger, key);

        List<String> lore = section.isList("lore") ? section.getStringList("lore")
                : defaults != null ? defaults.lore() : List.of();
        List<String> aliases = section.isList("aliases") ? section.getStringList("aliases")
                : defaults != null ? defaults.aliases() : List.of();

        long value = section.getLong("value", defaultValue(key));
        if (value < 1) {
            logger.warning("Moneda '" + key + "': value debe ser >= 1, se usará 1");
            value = 1;
        }

        NamespacedKey itemModel = null;
        String modelRaw = section.getString("item-model");
        if (modelRaw != null && !modelRaw.isBlank()) {
            itemModel = NamespacedKey.fromString(modelRaw.toLowerCase(Locale.ROOT));
            if (itemModel == null) {
                logger.warning("Moneda '" + key + "': item-model inválido '" + modelRaw + "'");
            }
        }
        boolean glow = section.getBoolean("glow", false);

        return new CurrencyDefinition(id, displayName, styled, color, material, lore, aliases, value, itemModel, glow);
    }

    private static void write(ConfigurationSection section, CurrencyDefinition def) {
        section.set("id", def.id());
        section.set("display-name", def.displayName());
        section.set("styled-display-name", def.styledDisplayName());
        section.set("color", def.color().asHexString());
        section.set("material", def.material().name());
        section.set("value", def.value());
        section.set("lore", def.lore());
        section.set("aliases", def.aliases());
        if (def.itemModel() != null) {
            section.set("item-model", def.itemModel().asString());
        }
        if (def.glow()) {
            section.set("glow", true);
        }
    }

    private static long defaultValue(String key) {
        return CurrencyDefaults.defaults().stream()
                .filter(def -> def.id().equalsIgnoreCase(key))
                .mapToLong(CurrencyDefinition::value)
                .findFirst()
                .orElse(1L);
    }

    public static @Nullable TextColor parseColor(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        NamedTextColor named = NamedTextColor.NAMES.value(value.toLowerCase(Locale.ROOT));
        if (named != null) {
            return named;
        }
        try {
            return TextColor.fromCSSHexString(value.startsWith("#") ? value : "#" + value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static TextColor parseColor(@Nullable String value, TextColor fallback, Logger logger, String key) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        TextColor parsed = parseColor(value);
        if (parsed == null) {
            logger.warning("Moneda '" + key + "': color desconocido '" + value + "', se usará " + fallback.asHexString());
            return fallback;
        }
        return parsed;
    }

    private static Material parseMaterial(@Nullable String value, Material fallback, Logger logger, String key) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(value);
        if (material == null || !material.isItem() || material.isAir()) {
            logger.warning("Moneda '" + key + "': material desconocido '" + value + "', se usará " + fallback);
            return fallback;
        }
        return material;
    }

    private static ConfigurationSection section(YamlConfiguration config) {
        ConfigurationSection root = config.getConfigurationSection(ROOT);
        return root != null ? root : config.createSection(ROOT);
    }

    private boolean save(YamlConfiguration config, File file) {
        try {
            config.save(file);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar " + FILE_NAME + ": " + ex.getMessage());
            return false;
        }
    }

    private File file() {
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("No se pudo crear la carpeta del plugin");
        }
        return new File(folder, FILE_NAME);
    }
}

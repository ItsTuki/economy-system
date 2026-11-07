package cl.auryx.economy.config;

import cl.auryx.economy.util.TextFormat;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/** Mensajes configurables en {@code messages.yml} (MiniMessage). */
public final class Messages {
    public static final String FILE_NAME = "messages.yml";

    private final Plugin plugin;
    private YamlConfiguration config = new YamlConfiguration();
    private TagResolver prefix = TagResolver.empty();

    public Messages(Plugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(FILE_NAME)) {
            if (in != null) {
                loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("No se pudieron leer los mensajes por defecto: " + ex.getMessage());
        }
        this.config = loaded;
        this.prefix = Placeholder.parsed("prefix", loaded.getString("prefix", ""));
    }

    public Component get(String key, TagResolver... resolvers) {
        String raw = config.getString(key, key);
        return TextFormat.miniMessage().deserialize(raw, TagResolver.resolver(prefix, TagResolver.resolver(resolvers)));
    }

    /** Envía el mensaje; si está vacío en messages.yml no envía nada. */
    public void send(Audience audience, String key, TagResolver... resolvers) {
        String raw = config.getString(key);
        if (raw == null) {
            raw = key;
        }
        if (raw.isEmpty()) {
            return;
        }
        audience.sendMessage(TextFormat.miniMessage().deserialize(raw, TagResolver.resolver(prefix, TagResolver.resolver(resolvers))));
    }

    public void sendList(Audience audience, String key, TagResolver... resolvers) {
        List<String> lines = config.getStringList(key);
        TagResolver all = TagResolver.resolver(prefix, TagResolver.resolver(resolvers));
        for (String line : lines) {
            audience.sendMessage(TextFormat.miniMessage().deserialize(line, all));
        }
    }
}

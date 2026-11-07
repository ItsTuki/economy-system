package cl.auryx.economy.util;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/** Banner de consola y créditos del plugin. */
public final class Credits {
    public static final String AUTHOR = "ItsTuki";
    public static final String REPOSITORY = "https://github.com/ItsTuki/economy-system";

    private static final String TITLE = "<gradient:#FFD122:#FF8D42><bold>EconomyItems</bold></gradient>";
    private static final String AUTHOR_TAG = "<gradient:#55FFFF:#5555FF><bold>" + AUTHOR + "</bold></gradient>";

    private Credits() {
    }

    /** Línea de créditos para los menús de ayuda, con enlace al repositorio. */
    public static Component helpLine() {
        return TextFormat.miniMessage().deserialize(
                "<dark_gray>» </dark_gray><gray>Creado por </gray>"
                        + "<click:open_url:'" + REPOSITORY + "'><hover:show_text:'<gray>Ver en GitHub'>"
                        + AUTHOR_TAG + "</hover></click>");
    }

    public static void enableBanner(Plugin plugin, int currencies) {
        send(plugin.getServer().getConsoleSender(), List.of(
                "",
                "<dark_gray>  ======================================",
                "  " + TITLE + " <gray>v<version>",
                "  <gray>Creado por " + AUTHOR_TAG,
                "  <gray>Monedas cargadas: <white><currencies></white> <dark_gray>|</dark_gray> <gray>Servidor: <white><server>",
                "  <green>Plugin activado correctamente.",
                "<dark_gray>  ======================================",
                ""
        ), plugin, currencies);
    }

    public static void disableBanner(Plugin plugin) {
        send(plugin.getServer().getConsoleSender(), List.of(
                "",
                "  " + TITLE + " <gray>v<version> <red>desactivado.",
                "  <gray>Gracias por usarlo. Creado por " + AUTHOR_TAG,
                ""
        ), plugin, 0);
    }

    private static void send(CommandSender console, List<String> lines, Plugin plugin, int currencies) {
        TagResolver resolver = TagResolver.resolver(
                Placeholder.unparsed("version", plugin.getPluginMeta().getVersion()),
                Placeholder.unparsed("currencies", String.valueOf(currencies)),
                Placeholder.unparsed("server", plugin.getServer().getName() + " " + plugin.getServer().getMinecraftVersion()));
        for (String line : lines) {
            console.sendMessage(TextFormat.miniMessage().deserialize(line, resolver));
        }
    }
}

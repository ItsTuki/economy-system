package cl.auryx.economy.util;

import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.Nullable;

/**
 * Convierte texto de configuración a componentes. Acepta tanto MiniMessage
 * ({@code <gold>Hola</gold>}) como el formato legacy con {@code &} y {@code &#RRGGBB}.
 */
public final class TextFormat {
    private static final Pattern MINI_MESSAGE_TAG = Pattern.compile("<[!/#a-zA-Z][^<>]*>");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private TextFormat() {
    }

    /** Interpreta el texto y lo deja sin cursiva (los nombres de ítem salen en cursiva por defecto). */
    public static Component item(@Nullable String text, @Nullable TextColor fallback) {
        Component component = parse(text);
        if (fallback != null) {
            component = component.colorIfAbsent(fallback);
        }
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static Component parse(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (MINI_MESSAGE_TAG.matcher(text).find() && text.indexOf('&') < 0) {
            return MINI_MESSAGE.deserialize(text);
        }
        return LEGACY.deserialize(text);
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }
}

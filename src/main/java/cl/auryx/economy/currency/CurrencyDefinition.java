package cl.auryx.economy.currency;

import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.Nullable;

/**
 * Definición inmutable de una moneda tal como aparece en {@code currencies.yml}.
 *
 * @param id                identificador persistente guardado en el ítem (no cambiar una vez en uso)
 * @param displayName       nombre plano, usado en mensajes y logs
 * @param styledDisplayName nombre con formato (MiniMessage o legacy {@code &#RRGGBB}) mostrado en el ítem
 * @param color             color por defecto si el nombre no define uno
 * @param material          material base del ítem
 * @param lore              líneas de descripción
 * @param aliases           nombres alternativos aceptados en comandos
 * @param value             valor relativo en unidades base (para cambio y saldo). Debe ser {@code >= 1}
 * @param itemModel         modelo de ítem personalizado (resource pack), o {@code null}
 * @param glow              si el ítem muestra brillo de encantamiento
 */
public record CurrencyDefinition(
        String id,
        String displayName,
        @Nullable String styledDisplayName,
        TextColor color,
        Material material,
        List<String> lore,
        List<String> aliases,
        long value,
        @Nullable NamespacedKey itemModel,
        boolean glow
) {
    public CurrencyDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Currency id cannot be null or empty");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Currency displayName cannot be null or empty");
        }
        if (color == null) {
            throw new IllegalArgumentException("Currency color cannot be null");
        }
        if (material == null || !material.isItem() || material.isAir()) {
            throw new IllegalArgumentException("Currency material must be a valid item");
        }
        if (value < 1) {
            throw new IllegalArgumentException("Currency value must be >= 1");
        }
        lore = lore == null ? List.of() : List.copyOf(lore);
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    /** Nombre con formato si existe, si no el nombre plano. */
    public String formattedName() {
        return styledDisplayName == null || styledDisplayName.isBlank() ? displayName : styledDisplayName;
    }
}

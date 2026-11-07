package cl.auryx.economy.currency;

import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public record CurrencyDefinition(
   String id, String displayName, String styledDisplayName, TextColor color, Material material, List<String> lore, List<String> aliases
) {
   public CurrencyDefinition(
      String id, String displayName, String styledDisplayName, TextColor color, Material material, List<String> lore, List<String> aliases
   ) {
      if (id == null || id.isEmpty()) {
         throw new IllegalArgumentException("Currency id cannot be null or empty");
      } else if (displayName == null || displayName.isEmpty()) {
         throw new IllegalArgumentException("Currency displayName cannot be null or empty");
      } else if (color == null) {
         throw new IllegalArgumentException("Currency color cannot be null");
      } else if (material == null) {
         throw new IllegalArgumentException("Currency material cannot be null");
      } else {
         lore = lore == null ? List.of() : List.copyOf(lore);
         aliases = aliases == null ? List.of() : List.copyOf(aliases);
         this.id = id;
         this.displayName = displayName;
         this.styledDisplayName = styledDisplayName;
         this.color = color;
         this.material = material;
         this.lore = lore;
         this.aliases = aliases;
      }
   }
}

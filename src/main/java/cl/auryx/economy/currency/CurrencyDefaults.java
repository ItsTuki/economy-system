package cl.auryx.economy.currency;

import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public final class CurrencyDefaults {
   private static final List<CurrencyDefinition> DEFAULTS = List.of(
      new CurrencyDefinition(
         "copper-coin",
         "Moneda de Cobre",
         "&#B33313&lM&#C63E11&lo&#D94A0F&ln&#EC550C&le&#FF600A&lda&#FF7726&l &#FF8D42&ld&#FFA45E&le&#FFBA7A&l C&#EC9860&lo&#D97747&lb&#C6552D&lr&#B33313&le",
         NamedTextColor.GOLD,
         Material.COPPER_NUGGET,
         List.of("&#CF5707Una moneda de cobre usada", "&#CF5707para comerciar."),
         List.of("cooper-coin", "cooper_coin", "copper_coin")
      ),
      new CurrencyDefinition(
         "silver-coin",
         "Moneda de Plata",
         "&#6F757A&lM&#797F84&lo&#84898E&ln&#8E9297&le&#989CA1&ld&#A2A6AB&la&#ADB0B5&l &#B7BAC0&ld&#C1C4CA&le&#CBCED4&l &#D5D8DF&lP&#DFE2E9&ll&#E9ECF3&la&#F3F5FC&lt&#FDFFFF&la",
         NamedTextColor.GRAY,
         Material.IRON_NUGGET,
         List.of("&#AEB5BDUna moneda de plata usada", "&#AEB5BDpara comerciar."),
         List.of("iron-coin", "iron_coin", "silver_coin")
      ),
      new CurrencyDefinition(
         "gold-coin",
         "Moneda de Oro",
         "&#B36F0A&lM&#BD7B0D&lo&#C78810&ln&#D19513&le&#DBA116&ld&#E5AE19&la&#EFBB1C&l &#F9C71F&ld&#FFD122&le&#FFD93A&l &#FFE150&lO&#FFE866&lr&#FFF07C&lo",
         NamedTextColor.YELLOW,
         Material.GOLD_NUGGET,
         List.of("&#E0AF1BUna moneda de oro usada", "&#E0AF1Bpara comerciar."),
         List.of("gold_coin", "golden-coin")
      ),
      new CurrencyDefinition(
         "platinum-coin",
         "Moneda de Platino",
         "&#A5F3FF&lM&#9BE8FF&lo&#91DDFF&ln&#87D2FF&le&#7DC7FF&ld&#73BCFF&la&#69B1FF&l &#5FA6FF&ld&#559BFF&le&#4B90FF&l &#4173FF&lP&#3756FF&ll&#2D3AFF&la&#231DFF&lt&#1900FF&li&#2F00FF&ln&#4500FF&lo",
         NamedTextColor.AQUA,
         Material.NETHER_STAR,
         List.of("&#63C8FFUna moneda de platino usada", "&#63C8FFpara comerciar."),
         List.of("platinum_coin", "platinum", "platino")
      )
   );

   private CurrencyDefaults() {
   }

   public static List<CurrencyDefinition> defaults() {
      return DEFAULTS;
   }
}

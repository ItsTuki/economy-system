package com.mc.economy.commands;

import cl.auryx.economy.currency.CurrencyDefinition;
import cl.auryx.economy.currency.CurrencyManager;
import com.mc.economy.EconomyPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class EconomyCommand implements CommandExecutor, TabCompleter {
   private static final String PERMISSION = "economy.admin";
   private final EconomyPlugin plugin;
   private final CurrencyManager currencyManager;

   public EconomyCommand(EconomyPlugin plugin, CurrencyManager currencyManager) {
      this.plugin = plugin;
      this.currencyManager = currencyManager;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("economy.admin")) {
         sender.sendMessage("No tienes permiso para usar este comando.");
         return true;
      } else if (args.length == 0) {
         this.sendUsage(sender);
         return true;
      } else {
         String subcommand = args[0].toLowerCase(Locale.ROOT);
         switch (subcommand) {
            case "list":
               this.handleList(sender);
               break;
            case "give":
               this.handleGive(sender, args);
               break;
            case "remove":
               this.handleRemove(sender, args);
               break;
            case "add":
               this.handleAdd(sender, args);
               break;
            case "reload":
               this.handleReload(sender);
               break;
            default:
               this.sendUsage(sender);
         }

         return true;
      }
   }

   private void handleList(CommandSender sender) {
      String available = this.currencyManager.getDefinitions().stream().map(def -> def.id() + " (" + def.displayName() + ")").collect(Collectors.joining(", "));
      sender.sendMessage("Monedas disponibles: " + available);
   }

   private void handleGive(CommandSender sender, String[] args) {
      if (args.length < 4) {
         sender.sendMessage("Uso: /economy give <jugador|@a> <moneda> <cantidad>");
      } else {
         boolean allPlayers = this.isAllSelector(args[1]);
         List<Player> targets = this.resolveTargets(args[1]);
         if (targets.isEmpty()) {
            sender.sendMessage(allPlayers ? "No hay jugadores en línea para recibir monedas." : "El jugador " + args[1] + " no está en línea.");
         } else {
            Optional<CurrencyDefinition> currencyDefinition = this.currencyManager.findCurrency(args[2]);
            if (currencyDefinition.isEmpty()) {
               sender.sendMessage("Moneda desconocida: " + args[2]);
            } else {
               int amount;
               try {
                  amount = Integer.parseInt(args[3]);
               } catch (NumberFormatException var10) {
                  sender.sendMessage("La cantidad debe ser un número entero positivo.");
                  return;
               }

               if (amount <= 0) {
                  sender.sendMessage("La cantidad debe ser mayor que cero.");
               } else {
                  CurrencyDefinition definition = currencyDefinition.get();

                  for (Player target : targets) {
                     this.distributeStacks(target, definition, amount);
                     target.sendMessage("Has recibido " + amount + " " + definition.displayName() + ".");
                  }

                  if (targets.size() == 1) {
                     Player target = targets.get(0);
                     sender.sendMessage("Se entregaron " + amount + " " + definition.displayName() + " a " + target.getName() + ".");
                  } else {
                     sender.sendMessage(
                        "Se entregaron " + amount + " " + definition.displayName() + " a todos los jugadores en línea (" + targets.size() + ")."
                     );
                  }
               }
            }
         }
      }
   }

   private void handleRemove(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage("Uso: /economy remove <jugador>");
      } else {
         Player target = Bukkit.getPlayerExact(args[1]);
         if (target == null) {
            sender.sendMessage("El jugador " + args[1] + " no está en línea.");
         } else {
            int removed = this.purgeCoins(target.getInventory());
            sender.sendMessage("Se eliminaron " + removed + " monedas del inventario de " + target.getName() + ".");
            if (removed > 0) {
               target.sendMessage("Tus monedas han sido retiradas por un administrador.");
            } else {
               target.sendMessage("No se encontraron monedas en tu inventario.");
            }
         }
      }
   }

   private void handleAdd(CommandSender sender, String[] args) {
      if (!sender.isOp()) {
         sender.sendMessage("Solo los operadores pueden crear nuevas divisas.");
      } else if (args.length < 5) {
         sender.sendMessage("Uso: /economy add <id> <material> <colorHex|nombre> <nombre mostrable>");
         sender.sendMessage("Ejemplo: /economy add rubi GEM '#FF0044' Moneda Rubí");
         sender.sendMessage("Nota: se agregará automáticamente el sufijo -coin al identificador.");
      } else {
         String id = this.ensureCoinSuffix(this.normalize(args[1]));
         if (this.currencyManager.exists(id)) {
            sender.sendMessage("Ya existe una divisa con ese identificador o alias.");
         } else {
            Material material = Material.matchMaterial(args[2].toUpperCase(Locale.ROOT));
            if (material == null) {
               sender.sendMessage("Material desconocido: " + args[2]);
            } else {
               TextColor color = this.parseColorArg(args[3]);
               if (color == null) {
                  sender.sendMessage("Color inválido. Usa nombres conocidos (gold, aqua) o hex (#FFAA00).");
               } else {
                  String displayName = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                  CurrencyDefinition definition = new CurrencyDefinition(
                     id, displayName, displayName, color, material, List.of("&7Divisa generada por " + sender.getName()), this.buildAliases(id)
                  );
                  if (this.plugin.addCurrency(definition)) {
                     sender.sendMessage("Se creó la divisa '" + displayName + "'. Edita currencies.yml para personalizarla.");
                  } else {
                     sender.sendMessage("No se pudo crear la divisa; verifica el log.");
                  }
               }
            }
         }
      }
   }

   private void handleReload(CommandSender sender) {
      this.plugin.reloadCurrencies();
      sender.sendMessage("Divisas recargadas desde currencies.yml.");
   }

   private void distributeStacks(Player target, CurrencyDefinition currencyDefinition, int totalAmount) {
      int remaining = totalAmount;
      int maxStackSize = 99;

      while (remaining > 0) {
         int stackAmount = Math.min(remaining, maxStackSize);
         ItemStack stack = this.currencyManager.createCurrency(currencyDefinition, stackAmount);
         Map<Integer, ItemStack> overflow = target.getInventory().addItem(new ItemStack[]{stack});
         if (!overflow.isEmpty()) {
            overflow.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
         }

         remaining -= stackAmount;
      }
   }

   private int purgeCoins(Inventory inventory) {
      if (inventory == null) {
         return 0;
      } else {
         int removed = 0;
         ItemStack[] contents = inventory.getContents();

         for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack != null && !stack.getType().isAir() && this.currencyManager.isCoinStack(stack)) {
               removed += stack.getAmount();
               inventory.clear(slot);
            }
         }

         return removed;
      }
   }

   private void sendUsage(CommandSender sender) {
      sender.sendMessage("/economy list");
      sender.sendMessage("/economy give <jugador|@a> <moneda> <cantidad>");
      sender.sendMessage("/economy remove <jugador>");
      sender.sendMessage("/economy add <id> <material> <color> <nombre>");
      sender.sendMessage("/economy reload");
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (!sender.hasPermission("economy.admin")) {
         return Collections.emptyList();
      } else if (args.length == 1) {
         return this.filter(List.of("list", "give", "remove", "add", "reload"), args[0]);
      } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
         List<String> players = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
         players.add("@a");
         return this.filter(players, args[1]);
      } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
         List<String> players = Bukkit.getOnlinePlayers().stream().<String>map(Player::getName).toList();
         return this.filter(players, args[1]);
      } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
         List<String> currencies = this.currencyManager.getDefinitions().stream().map(CurrencyDefinition::id).toList();
         return this.filter(currencies, args[2]);
      } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
         return this.filter(List.of("1", "16", "64", "99"), args[3]);
      } else {
         if (args[0].equalsIgnoreCase("add")) {
            if (args.length == 3) {
               return this.filter(this.suggestMaterials(), args[2]);
            }

            if (args.length == 4) {
               return this.filter(List.of("#FFAA00", "#55FFFF", "gold", "aqua"), args[3]);
            }
         }

         return Collections.emptyList();
      }
   }

   private List<String> filter(List<String> values, String current) {
      if (current != null && !current.isEmpty()) {
         String lower = current.toLowerCase(Locale.ROOT);
         return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).collect(Collectors.toList());
      } else {
         return values;
      }
   }

   private TextColor parseColorArg(String value) {
      NamedTextColor named = (NamedTextColor)NamedTextColor.NAMES.value(value.toLowerCase(Locale.ROOT));
      if (named != null) {
         return named;
      } else {
         try {
            return TextColor.fromCSSHexString(value.startsWith("#") ? value : "#" + value);
         } catch (IllegalArgumentException var4) {
            return null;
         }
      }
   }

   private List<String> suggestMaterials() {
      return List.of("GOLD_NUGGET", "IRON_NUGGET", "EMERALD", "NETHER_STAR");
   }

   private List<Player> resolveTargets(String token) {
      if (this.isAllSelector(token)) {
         return List.copyOf(Bukkit.getOnlinePlayers());
      } else {
         Player player = Bukkit.getPlayerExact(token);
         return player != null ? List.of(player) : List.of();
      }
   }

   private boolean isAllSelector(String token) {
      return token != null && token.equalsIgnoreCase("@a");
   }

   private String normalize(String value) {
      return value.toLowerCase(Locale.ROOT).replace(' ', '-').replace('_', '-');
   }

   private String ensureCoinSuffix(String value) {
      String normalized = value == null ? "" : value.replace('_', '-');
      String base = this.stripCoinSuffix(normalized);
      if (base.isEmpty()) {
         base = normalized.isEmpty() ? "currency" : normalized;
      }

      return base + "-coin";
   }

   private List<String> buildAliases(String id) {
      String base = this.stripCoinSuffix(id);
      return !base.isEmpty() && !base.equals(id) ? List.of(id, base) : List.of(id);
   }

   private String stripCoinSuffix(String value) {
      if (value != null && !value.isEmpty()) {
         String base = value.replace('_', '-');
         if (base.endsWith("-coin")) {
            base = base.substring(0, base.length() - 5);
         }

         while (base.endsWith("-")) {
            base = base.substring(0, base.length() - 1);
         }

         return base;
      } else {
         return "";
      }
   }
}

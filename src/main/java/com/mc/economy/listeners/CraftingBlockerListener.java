package com.mc.economy.listeners;

import cl.auryx.economy.currency.CurrencyManager;
import java.util.Locale;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public final class CraftingBlockerListener implements Listener {
   private final CurrencyManager currencyManager;

   public CraftingBlockerListener(CurrencyManager currencyManager) {
      this.currencyManager = currencyManager;
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onPrepare(PrepareItemCraftEvent event) {
      CraftingInventory inventory = event.getInventory();
      if (this.shouldBlockRecipe(event.getRecipe()) || this.currencyManager.containsCurrency(inventory.getMatrix())) {
         inventory.setResult(new ItemStack(Material.AIR));
      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onCraft(CraftItemEvent event) {
      boolean blockedRecipe = this.shouldBlockRecipe(event.getRecipe());
      boolean containsCurrency = this.currencyManager.containsCurrency(event.getInventory().getMatrix());
      if (blockedRecipe || containsCurrency) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player player) {
            player.sendMessage(blockedRecipe ? "Las monedas no pueden fabricarse mediante recetas." : "Los objetos monetarios no pueden usarse en recetas.");
         }
      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onCrafterCraft(CrafterCraftEvent event) {
      boolean blockedRecipe = this.shouldBlockRecipe(event.getRecipe());
      Inventory inventory = this.extractCrafterInventory(event);
      boolean containsCurrency = inventory != null && this.currencyManager.containsCurrency(inventory.getContents());
      if (blockedRecipe || containsCurrency) {
         event.setCancelled(true);
         event.setResult(new ItemStack(Material.AIR));
      }
   }

   private Inventory extractCrafterInventory(CrafterCraftEvent event) {
      return event.getBlock().getState() instanceof Crafter crafter ? crafter.getInventory() : null;
   }

   private boolean shouldBlockRecipe(Recipe recipe) {
      if (recipe == null) {
         return false;
      } else if (recipe instanceof Keyed keyed) {
         NamespacedKey key = keyed.getKey();
         String path = key != null ? key.getKey() : null;
         return path != null && path.toLowerCase(Locale.ROOT).endsWith("-coin");
      } else {
         return false;
      }
   }
}

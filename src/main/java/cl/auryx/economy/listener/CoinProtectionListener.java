package cl.auryx.economy.listener;

import cl.auryx.economy.EconomyPlugin;
import cl.auryx.economy.config.Settings;
import cl.auryx.economy.currency.CurrencyManager;
import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import java.util.Locale;
import org.bukkit.Keyed;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.Recipe;
import org.jspecify.annotations.Nullable;

/** Evita que las monedas se fabriquen, se consuman o se transformen en otra cosa. */
public final class CoinProtectionListener implements Listener {
    private final EconomyPlugin plugin;
    private final CurrencyManager coins;

    public CoinProtectionListener(EconomyPlugin plugin, CurrencyManager coins) {
        this.plugin = plugin;
        this.coins = coins;
    }

    private Settings.Protection protection() {
        return plugin.settings().protection();
    }

    // ── Mesa de crafteo ──────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!protection().crafting()) {
            return;
        }
        if (isCoinRecipe(event.getRecipe()) || coins.containsCoin(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!protection().crafting()) {
            return;
        }
        boolean coinRecipe = isCoinRecipe(event.getRecipe());
        if (coinRecipe || coins.containsCoin(event.getInventory().getMatrix())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                plugin.messages().send(player, coinRecipe ? "protection.recipe" : "protection.crafting");
            }
        }
    }

    // ── Crafter ──────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (!protection().crafter()) {
            return;
        }
        boolean blocked = isCoinRecipe(event.getRecipe());
        if (!blocked && event.getBlock().getState(false) instanceof Crafter crafter) {
            blocked = coins.containsCoin(crafter.getInventory().getContents());
        }
        if (blocked) {
            event.setCancelled(true);
        }
    }

    // ── Yunque, afiladora, herrería, telar, cartografía ──────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareResult(PrepareResultEvent event) {
        if (!protection().workstations()) {
            return;
        }
        Inventory inventory = event.getInventory();
        if (coins.containsCoin(inventory.getContents())) {
            event.setResult(null);
        }
    }

    // ── Hornos ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (protection().furnaces() && coins.isCoin(event.getFuel())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (protection().furnaces() && coins.isCoin(event.getSource())) {
            event.setCancelled(true);
        }
    }

    // ── Aldeanos y comerciantes ──────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTrade(PlayerTradeEvent event) {
        if (!protection().villagerTrades()) {
            return;
        }
        if (event.getPlayer().getOpenInventory().getTopInventory() instanceof MerchantInventory merchant
                && (coins.isCoin(merchant.getItem(0)) || coins.isCoin(merchant.getItem(1)))) {
            event.setCancelled(true);
        }
    }

    // ── Colocar y consumir ───────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (protection().placing() && coins.isCoin(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (protection().consuming() && coins.isCoin(event.getItem())) {
            event.setCancelled(true);
        }
    }

    private static boolean isCoinRecipe(@Nullable Recipe recipe) {
        return recipe instanceof Keyed keyed
                && keyed.getKey().getKey().toLowerCase(Locale.ROOT).endsWith(CurrencyManager.COIN_SUFFIX);
    }
}

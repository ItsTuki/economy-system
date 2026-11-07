package cl.auryx.economy.listener;

import cl.auryx.economy.EconomyPlugin;
import cl.auryx.economy.currency.CurrencyManager;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/** Cuida las monedas tiradas en el mundo y mantiene al día las del inventario. */
public final class CoinItemListener implements Listener {
    private final EconomyPlugin plugin;
    private final CurrencyManager coins;

    public CoinItemListener(EconomyPlugin plugin, CurrencyManager coins) {
        this.plugin = plugin;
        this.coins = coins;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (!coins.isCoin(item.getItemStack())) {
            return;
        }
        if (plugin.settings().droppedNeverDespawn()) {
            item.setUnlimitedLifetime(true);
        }
        if (plugin.settings().droppedInvulnerable()) {
            item.setInvulnerable(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.settings().refreshOnJoin()) {
            coins.refresh(event.getPlayer().getInventory());
        }
    }
}

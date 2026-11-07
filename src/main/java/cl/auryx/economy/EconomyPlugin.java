package cl.auryx.economy;

import cl.auryx.economy.command.EconomyAdminCommand;
import cl.auryx.economy.command.PlayerCoinsCommand;
import cl.auryx.economy.config.Messages;
import cl.auryx.economy.config.Settings;
import cl.auryx.economy.currency.CurrencyConfigLoader;
import cl.auryx.economy.currency.CurrencyDefinition;
import cl.auryx.economy.currency.CurrencyManager;
import cl.auryx.economy.listener.CoinItemListener;
import cl.auryx.economy.listener.CoinProtectionListener;
import cl.auryx.economy.util.Credits;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyPlugin extends JavaPlugin {
    private CurrencyConfigLoader currencyLoader;
    private CurrencyManager currencyManager;
    private Messages messages;
    private Settings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.currencyLoader = new CurrencyConfigLoader(this);
        this.currencyManager = new CurrencyManager(this);
        this.messages = new Messages(this);
        reloadAll();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new CoinProtectionListener(this, currencyManager), this);
        pm.registerEvents(new CoinItemListener(this, currencyManager), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(new EconomyAdminCommand(this).build(),
                    "Administra las monedas del servidor.", List.of("economia"));
            event.registrar().register(new PlayerCoinsCommand(this).build(),
                    "Consulta, cambia y paga con tus monedas.", List.of("monedas"));
        });

        Credits.enableBanner(this, currencyManager.definitions().size());
    }

    @Override
    public void onDisable() {
        Credits.disableBanner(this);
    }

    /** Recarga config.yml, messages.yml y currencies.yml. */
    public void reloadAll() {
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        this.settings = Settings.from(getConfig());
        messages.reload();
        currencyManager.setMaxStackSize(settings.maxStackSize());
        reloadCurrencies();
        if (settings.refreshOnJoin()) {
            for (Player player : getServer().getOnlinePlayers()) {
                currencyManager.refresh(player.getInventory());
            }
        }
    }

    public void reloadCurrencies() {
        List<CurrencyDefinition> definitions = currencyLoader.load();
        currencyManager.setDefinitions(definitions);
        if (definitions.isEmpty()) {
            getLogger().warning("No hay monedas válidas en " + CurrencyConfigLoader.FILE_NAME);
        }
    }

    public boolean addCurrency(CurrencyDefinition definition) {
        boolean created = currencyLoader.add(definition);
        if (created) {
            reloadCurrencies();
        }
        return created;
    }

    public CurrencyManager currencies() {
        return currencyManager;
    }

    public Messages messages() {
        return messages;
    }

    public Settings settings() {
        return settings;
    }
}

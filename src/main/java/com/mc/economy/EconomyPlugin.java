package com.mc.economy;

import cl.auryx.economy.currency.CurrencyConfigLoader;
import cl.auryx.economy.currency.CurrencyDefinition;
import cl.auryx.economy.currency.CurrencyManager;
import java.util.List;
import com.mc.economy.commands.EconomyCommand;
import com.mc.economy.listeners.CraftingBlockerListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyPlugin extends JavaPlugin {
    private final CurrencyConfigLoader currencyConfigLoader = new CurrencyConfigLoader();
    private CurrencyManager currencyManager;

    @Override
    public void onEnable() {
        this.currencyManager = new CurrencyManager(this);
        reloadCurrencies();

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CraftingBlockerListener(currencyManager), this);

        EconomyCommand economyCommand = new EconomyCommand(this, currencyManager);
        PluginCommand pluginCommand = getCommand("economy");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(economyCommand);
            pluginCommand.setTabCompleter(economyCommand);
        } else {
            getLogger().severe("Command 'economy' is missing from plugin.yml");
        }

        getLogger().info("Economy plugin enabled with custom currency items.");
    }

    public void reloadCurrencies() {
        List<CurrencyDefinition> definitions = currencyConfigLoader.load(this);
        if (currencyManager != null) {
            currencyManager.setDefinitions(definitions);
        }
        getLogger().info("Currency definitions reloaded from currencies.yml");
    }

    public boolean addCurrency(CurrencyDefinition definition) {
        boolean created = currencyConfigLoader.addCurrency(this, definition);
        if (created) {
            reloadCurrencies();
        }
        return created;
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }
}

package stark.skyBlockTest2.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import stark.skyBlockTest2.SkyBlockTest2;

import java.util.UUID;

public class EconomyManager {

    private final SkyBlockTest2 plugin;
    private VaultEconomyProvider provider;
    private Economy economy;

    public EconomyManager(SkyBlockTest2 plugin) {
        this.plugin = plugin;
    }

    /**
     * Tworzy i rejestruje własny provider ekonomii w Vault.
     * Wywołać w onEnable po połączeniu z DB.
     *
     * @return zawsze true (własna implementacja, nie wymaga zewnętrznych pluginów)
     */
    public boolean setup() {
        String currency = plugin.getConfig().getString("economy.currency", "Monet");

        provider = new VaultEconomyProvider(plugin.getDatabaseManager(), currency);

        plugin.getServer().getServicesManager().register(
                Economy.class, provider, plugin, ServicePriority.Highest
        );

        economy = provider;
        plugin.getLogger().info("[Economy] Zarejestrowano własny provider: SkyBlockEconomy (waluta: " + currency + ")");
        return true;
    }

    // -------------------------------------------------------------------------
    // Cache lifecycle — wołaj z EconomyListener
    // -------------------------------------------------------------------------

    public void loadPlayer(UUID uuid) {
        if (provider != null) provider.loadPlayer(uuid);
    }

    public void unloadPlayer(UUID uuid) {
        if (provider != null) provider.unloadPlayer(uuid);
    }

    // -------------------------------------------------------------------------
    // Delegaty — zachowują kompatybilność z resztą pluginu
    // -------------------------------------------------------------------------

    public boolean has(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        return economy != null && economy.depositPlayer(player, amount).transactionSuccess();
    }

    public void depositOffline(UUID uuid, double amount) {
        if (provider != null) provider.depositPlayer(plugin.getServer().getOfflinePlayer(uuid), amount);
    }

    public double getBalance(Player player) {
        return economy == null ? 0 : economy.getBalance(player);
    }

    public String format(double amount) {
        return economy == null ? String.format("%.2f", amount) : economy.format(amount);
    }

    /** Ustawia saldo (admin). Działa też dla offline graczy. */
    public void setBalance(UUID uuid, double amount) {
        if (provider != null) provider.setBalance(uuid, amount);
    }

    public void reload() {
        if (provider == null) return;
        String currency = plugin.getConfig().getString("economy.currency", "Monet");
        provider.setCurrency(currency);
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }
}

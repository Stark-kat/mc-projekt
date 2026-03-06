package stark.skyBlockTest2.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import stark.skyBlockTest2.SkyBlockTest2;

import java.util.logging.Level;

public class EconomyManager {

    private Economy economy;
    private final SkyBlockTest2 plugin;

    public EconomyManager(SkyBlockTest2 plugin) {
        this.plugin = plugin;
    }

    /**
     * Szuka providera ekonomii (np. EssentialsX) przez Vault.
     * Wywołać w onEnable po załadowaniu wszystkich pluginów.
     *
     * @return true jeśli provider został znaleziony
     */
    public boolean setup() {
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().log(Level.SEVERE,
                    "[Economy] Nie znaleziono providera ekonomii! " +
                            "Zainstaluj EssentialsX lub inny plugin implementujący Vault Economy.");
            return false;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("[Economy] Połączono z providerem: " + economy.getName());
        return true;
    }

    /** Czy gracz ma wystarczające środki? */
    public boolean has(Player player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }

    /** Odejmuje środki. Zwraca true jeśli transakcja się powiodła. */
    public boolean withdraw(Player player, double amount) {
        if (economy == null) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /** Aktualny balans gracza. */
    public double getBalance(Player player) {
        if (economy == null) return 0;
        return economy.getBalance(player);
    }

    /** Formatuje kwotę zgodnie z walutą providera (np. "5 000,00 $"). */
    public String format(double amount) {
        if (economy == null) return String.format("%.2f", amount);
        return economy.format(amount);
    }

    public boolean isAvailable() {
        return economy != null;
    }
}
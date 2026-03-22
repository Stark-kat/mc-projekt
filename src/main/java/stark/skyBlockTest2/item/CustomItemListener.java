package stark.skyBlockTest2.item;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.crate.CrateAnimationGui;
import stark.skyBlockTest2.crate.CrateDefinition;
import stark.skyBlockTest2.crate.CrateManager;
import stark.skyBlockTest2.island.IslandManager;

import java.util.UUID;

public class CustomItemListener implements Listener {

    private final SkyBlockTest2 plugin;
    private final IslandManager islandManager;
    private final CrateManager crateManager;
    private final CrateAnimationGui crateAnimationGui;

    public CustomItemListener(SkyBlockTest2 plugin, IslandManager islandManager,
                              CrateManager crateManager, CrateAnimationGui crateAnimationGui) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.crateManager = crateManager;
        this.crateAnimationGui = crateAnimationGui;
    }

    // =========================================================================
    // Stawianie spawnerów — tylko w wymaganym świecie
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String customId = CustomItemRegistry.getId(item);
        if (customId == null || !customId.startsWith("spawner_")) return;

        String worldName     = event.getBlock().getWorld().getName();
        String requiredWorld = CustomItemRegistry.getRequiredWorld(customId);

        if (!worldName.equals(requiredWorld)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    "§cTen spawner mozna postawic tylko na §e"
                            + CustomItemRegistry.getWorldLabel(requiredWorld) + "§c!");
        }
    }

    // =========================================================================
    // Kliknięcia — akcje custom itemów
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;

        String customId = CustomItemRegistry.getId(item);
        if (customId == null) return;

        var meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey actionKey = new NamespacedKey(plugin, "action_on_use");
        String actionOnUse = meta.getPersistentDataContainer()
                .get(actionKey, PersistentDataType.STRING);
        if (actionOnUse == null) return;

        event.setCancelled(true);
        handleAction(event.getPlayer(), customId, actionOnUse, item);
    }

    // =========================================================================
    // Obsługa akcji
    // =========================================================================

    private void handleAction(Player player, String customId, String action, ItemStack item) {
        switch (action) {
            case "island_key_use" -> {
                player.sendMessage("§6Uzywasz §eKlucza Wyspy§6...");
                player.sendMessage("§8(Akcja nie jest jeszcze zaimplementowana)");
            }
            case "open_crate" -> {
                NamespacedKey crateKey = new NamespacedKey(plugin, "crate_id");
                String crateId = item.getItemMeta().getPersistentDataContainer()
                        .get(crateKey, PersistentDataType.STRING);
                if (crateId == null) {
                    player.sendMessage("§cBlad: brak ID skrzynki!");
                    return;
                }
                CrateDefinition crate = crateManager.getCrate(crateId);
                if (crate == null) {
                    player.sendMessage("§cBlad: nieznana skrzynka '" + crateId + "'!");
                    return;
                }
                crateAnimationGui.open(player, crate);
            }
            default -> plugin.getLogger().warning(
                    "[CustomItemListener] Nieznana akcja: " + action + " dla: " + customId);
        }
    }

    // =========================================================================
    // Obsługa rozłączenia i śmierci podczas animacji
    // =========================================================================

    /**
     * Quit i kick odpala ten sam event — jedno miejsce obsługuje oba przypadki.
     * Gracz jest jeszcze online gdy event odpala się, więc addItem działa.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!crateAnimationGui.isAnimating(uuid)) return;

        crateAnimationGui.forceFinish(uuid);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        if (!crateAnimationGui.isAnimating(uuid)) return;

        crateAnimationGui.forceFinish(uuid);
    }

    /**
     * Przy logowaniu sprawdzamy czy gracz ma nieoddaną nagrodę z poprzedniej sesji
     * (np. serwer zrestartował się w trakcie animacji).
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        crateAnimationGui.deliverPendingRewardOnJoin(event.getPlayer());
    }
}
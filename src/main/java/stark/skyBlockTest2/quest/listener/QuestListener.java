package stark.skyBlockTest2.quest.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import stark.skyBlockTest2.quest.QuestManager;
import stark.skyBlockTest2.quest.QuestTrigger;

public class QuestListener implements Listener {

    private final QuestManager questManager;
    private final JavaPlugin plugin;

    public QuestListener(QuestManager questManager, JavaPlugin plugin) {
        this.questManager = questManager;
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Kopanie bloków
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        String material = event.getBlock().getType().name();
        questManager.addProgress(event.getPlayer(), QuestTrigger.BREAK_BLOCK, material, 1);
    }

    // -------------------------------------------------------------------------
    // Zabijanie mobów
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String entityType = event.getEntity().getType().name();
        questManager.addProgress(killer, QuestTrigger.KILL_MOB, entityType, 1);
    }

    // -------------------------------------------------------------------------
    // Łowienie ryb (liczy się złapany przedmiot lub ryba)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        String itemType = "";
        if (event.getCaught() instanceof Item caughtItem) {
            itemType = caughtItem.getItemStack().getType().name();
        }

        questManager.addProgress(event.getPlayer(), QuestTrigger.FISH, itemType, 1);
    }

    // -------------------------------------------------------------------------
    // Craftowanie
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String material = event.getCurrentItem().getType().name();

        // Przy shift-click gracz bierze wiele stacków naraz — szacujemy ilość
        int amount;
        if (event.isShiftClick()) {
            // Liczymy ile razy przepis można powtórzyć z dostępnych składników
            amount = calculateShiftCraftAmount(event);
        } else {
            amount = event.getCurrentItem().getAmount();
        }

        questManager.addProgress(player, QuestTrigger.CRAFT_ITEM, material, amount);
    }

    // -------------------------------------------------------------------------
    // Stawianie bloków
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        String material = event.getBlock().getType().name();
        questManager.addProgress(event.getPlayer(), QuestTrigger.PLACE_BLOCK, material, 1);
    }

    // -------------------------------------------------------------------------
    // Wytapianie (gracz wyciąga ze skrzynki wyjściowej pieca)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
        questManager.addProgress(event.getPlayer(), QuestTrigger.SMELT_ITEM,
                event.getItemType().name(), event.getItemAmount());
    }

    // -------------------------------------------------------------------------
    // Handel z wieśniakiem (klik w slot wynikowy MerchantInventory)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory() instanceof MerchantInventory)) return;
        if (event.getRawSlot() != 2) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
        questManager.addProgress(player, QuestTrigger.TRADE, "", 1);
    }

    // -------------------------------------------------------------------------
    // Czat
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        // AsyncPlayerChatEvent odpala na wątku async — przenosimy na main
        Bukkit.getScheduler().runTask(plugin,
                () -> questManager.addProgress(player, QuestTrigger.SEND_CHAT, "", 1));
    }

    // -------------------------------------------------------------------------
    // Jedzenie (zwykłe przedmioty)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEatFood(PlayerItemConsumeEvent event) {
        if (!event.getItem().getType().isEdible()) return;
        questManager.addProgress(event.getPlayer(), QuestTrigger.EAT_FOOD,
                event.getItem().getType().name(), 1);
    }

    // -------------------------------------------------------------------------
    // Jedzenie tortu (right-click na blok CAKE)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEatCake(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.CAKE) return;
        questManager.addProgress(event.getPlayer(), QuestTrigger.EAT_FOOD, "CAKE", 1);
    }

    // -------------------------------------------------------------------------
    // Przeżycie upadku (EntityDamageEvent BEFORE damage is applied)
    // -------------------------------------------------------------------------

    private static final double MIN_FALL_DAMAGE = 5.0;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSurviveFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getFinalDamage() < MIN_FALL_DAMAGE) return;
        // Sprawdź czy gracz przeżyje (health jest w skali 0-20 dla surowego zdrowia)
        if (player.getHealth() - event.getFinalDamage() > 0) {
            questManager.addProgress(player, QuestTrigger.SURVIVE_FALL, "", 1);
        }
    }

    /**
     * Szacuje liczbę crafted items przy shift-click.
     * Sprawdza minimalną ilość składników w slocie crafting grid.
     */
    private int calculateShiftCraftAmount(CraftItemEvent event) {
        int minIngredient = Integer.MAX_VALUE;
        for (var item : event.getInventory().getMatrix()) {
            if (item != null && !item.getType().isAir()) {
                minIngredient = Math.min(minIngredient, item.getAmount());
            }
        }
        if (minIngredient == Integer.MAX_VALUE) minIngredient = 1;
        return minIngredient * (event.getCurrentItem() != null ? event.getCurrentItem().getAmount() : 1);
    }
}
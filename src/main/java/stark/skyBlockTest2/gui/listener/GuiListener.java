package stark.skyBlockTest2.gui.listener;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.Spawn.TeleportManager;
import stark.skyBlockTest2.gui.menu.*;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.listener.IslandProtectionListener;

import java.util.UUID;

public class GuiListener implements Listener {

    private final TeleportManager teleportManager;
    private final IslandManager islandManager;
    private final SkyBlockTest2 plugin;
    private final MenuGui menuGui;
    private final UpgradeIslandGui upgradeIslandGui;
    private final MembersGui membersGui;
    private final MemberSettingsGui memberSettingsGui;
    private final IslandSettingsGui islandSettingsGui;

    public GuiListener(TeleportManager teleportManager,
                       IslandManager islandManager,
                       SkyBlockTest2 plugin,
                       MenuGui menuGui,
                       UpgradeIslandGui upgradeIslandGui,
                       MembersGui membersGui,
                       MemberSettingsGui memberSettingsGui,
                       IslandSettingsGui islandSettingsGui) {
        this.teleportManager = teleportManager;
        this.islandManager = islandManager;
        this.plugin = plugin;
        this.menuGui = menuGui;
        this.upgradeIslandGui = upgradeIslandGui;
        this.membersGui = membersGui;
        this.memberSettingsGui = memberSettingsGui;
        this.islandSettingsGui = islandSettingsGui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!(e.getWhoClicked() instanceof Player player)) return;

        Inventory topInventory = e.getView().getTopInventory();
        if (topInventory == null) return;

        // 🔥 Najważniejsze sprawdzenie
        if (!(topInventory.getHolder() instanceof MenuHolder)) return;

        // Blokujemy wszystkie interakcje z GUI
        e.setCancelled(true);

        // Blokujemy shift + klik z ekwipunku gracza
        if (e.getClickedInventory() == null) return;
        if (e.getClickedInventory() != topInventory) return;

        ItemStack item = e.getCurrentItem();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();

        NamespacedKey key = new NamespacedKey(plugin, "action");

        if (!container.has(key, PersistentDataType.STRING)) return;

        String action = container.get(key, PersistentDataType.STRING);
        String targetUuidStr = container.get(new NamespacedKey(plugin, "target_uuid"), PersistentDataType.STRING);

        switch (action) {

            case "TeleportSpawn" -> {
                if (!teleportManager.hasSpawn()) {
                    player.sendMessage("§cSpawn nie ustawiony");
                    return;
                }
                teleportManager.teleportWithDelay(player);
                player.closeInventory();
            }

            case "MenuGui" -> { menuGui.open(player);}

            case "SetDay" -> {
                player.getWorld().setTime(24000);
                player.closeInventory();
            }

            case "TeleportHome" -> {
                islandManager.teleportHome(player);
                player.closeInventory();
            }

            case "CreateIsland" -> {
                islandManager.createIsland(player);
                player.closeInventory();
            }

            case "CloseGui" -> player.closeInventory();

            case "lvl1" -> {
                islandManager.setIslandLevel(player, 1);
                player.closeInventory();
            }
            case "lvl2" -> {
                islandManager.setIslandLevel(player, 2);
                player.closeInventory();
            }
            case "lvl3" -> {
                islandManager.setIslandLevel(player, 3);
                player.closeInventory();
            }
            case "lvl4" -> {
                islandManager.setIslandLevel(player, 4);
                player.closeInventory();
            }
            case "lvl5" -> {
                islandManager.setIslandLevel(player, 5);
                player.closeInventory();
            }
            case "menuSizeLvl" -> upgradeIslandGui.open(player);
            case "members" -> membersGui.open(player);
            case "MemberInfo" -> {
                if (targetUuidStr == null) return;

                OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(targetUuidStr));

                memberSettingsGui.open(player, target);
            }
            case "PromoteToLeader" -> {
                if (targetUuidStr == null) return;
                UUID targetUUID = UUID.fromString(targetUuidStr);

                islandManager.transferOwnership(player, targetUUID);

                player.closeInventory();
            }
            case "KickMember" -> {
                if (targetUuidStr == null) return;
                UUID targetUUID = UUID.fromString(targetUuidStr);

                islandManager.kickMember(player, targetUUID);

                membersGui.open(player);
            }
            case "ChangeCategory" -> {
                String catName = container.get(new NamespacedKey(plugin, "category_name"), PersistentDataType.STRING);
                if (catName != null) {
                    islandSettingsGui.open(player, IslandProtectionListener.ActionCategory.valueOf(catName));
                }
            }
            case "ToggleIslandSetting" -> {
                // 1. Co zmieniamy?
                String actionName = container.get(new NamespacedKey(plugin, "island_action"), PersistentDataType.STRING);
                // 2. W jakiej kategorii jesteśmy? (Pamięć)
                String currentCatName = container.get(new NamespacedKey(plugin, "current_category"), PersistentDataType.STRING);

                Island island = islandManager.getIsland(player.getUniqueId());
                if (island == null || actionName == null) return;

                try {
                    IslandProtectionListener.IslandAction targetAction = IslandProtectionListener.IslandAction.valueOf(actionName);

                    boolean newState = !island.canVisitorDo(targetAction);
                    island.setVisitorSetting(targetAction, newState);
                    islandManager.getStorage().saveIsland(island);

                    // Powrót do tej samej kategorii
                    IslandProtectionListener.ActionCategory cat = (currentCatName != null) ? IslandProtectionListener.ActionCategory.valueOf(currentCatName) : IslandProtectionListener.ActionCategory.GENERAL;
                    islandSettingsGui.open(player, cat);

                    player.sendMessage("§8» §7Zmieniono status §e" + targetAction.getDisplayName());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            case "OpenSettings" -> islandSettingsGui.open(player, IslandProtectionListener.ActionCategory.GENERAL);
        }
    }
}
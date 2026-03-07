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
import stark.skyBlockTest2.teleport.TeleportManager;
import stark.skyBlockTest2.gui.menu.*;
import stark.skyBlockTest2.island.ActionCategory;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandAction;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.IslandType;

import java.util.UUID;
import java.util.logging.Level;

public class GuiListener implements Listener {

    private final TeleportManager teleportManager;
    private final IslandManager islandManager;
    private final SkyBlockTest2 plugin;
    private final MenuGui menuGui;
    private final UpgradeIslandGui upgradeIslandGui;
    private final MembersGui membersGui;
    private final MemberSettingsGui memberSettingsGui;
    private final IslandSettingsGui islandSettingsGui;
    private final BannedPlayersGui bannedPlayersGui;
    private final IslandHubGui islandHubGui;
    private final IslandTypeSettingsGui islandTypeSettingsGui;
    private final IslandTypeUpgradeGui islandTypeUpgradeGui;

    public GuiListener(TeleportManager teleportManager,
                       IslandManager islandManager,
                       SkyBlockTest2 plugin,
                       MenuGui menuGui,
                       UpgradeIslandGui upgradeIslandGui,
                       MembersGui membersGui,
                       MemberSettingsGui memberSettingsGui,
                       IslandSettingsGui islandSettingsGui,
                       BannedPlayersGui bannedPlayersGui,
                       IslandHubGui islandHubGui,
                       IslandTypeSettingsGui islandTypeSettingsGui,
                       IslandTypeUpgradeGui islandTypeUpgradeGui) {
        this.teleportManager      = teleportManager;
        this.islandManager        = islandManager;
        this.plugin               = plugin;
        this.menuGui              = menuGui;
        this.upgradeIslandGui     = upgradeIslandGui;
        this.membersGui           = membersGui;
        this.memberSettingsGui    = memberSettingsGui;
        this.islandSettingsGui    = islandSettingsGui;
        this.bannedPlayersGui     = bannedPlayersGui;
        this.islandHubGui         = islandHubGui;
        this.islandTypeSettingsGui = islandTypeSettingsGui;
        this.islandTypeUpgradeGui  = islandTypeUpgradeGui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Inventory topInventory = e.getView().getTopInventory();
        if (topInventory == null) return;

        // Tylko nasze GUI
        if (!(topInventory.getHolder() instanceof MenuHolder)) return;

        // Blokujemy WSZYSTKIE interakcje z GUI (łącznie z shift+click z ekwipunku)
        e.setCancelled(true);

        if (e.getClickedInventory() == null) return;
        if (e.getClickedInventory() != topInventory) return;

        ItemStack item = e.getCurrentItem();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        if (!container.has(actionKey, PersistentDataType.STRING)) return;

        String action        = container.get(actionKey, PersistentDataType.STRING);
        String targetUuidStr = container.get(new NamespacedKey(plugin, "target_uuid"), PersistentDataType.STRING);

        switch (action) {

            case "TeleportSpawn" -> {
                if (!teleportManager.hasSpawn()) {
                    player.sendMessage("§cSpawn nie jest ustawiony.");
                    return;
                }
                teleportManager.teleportWithDelay(player);
                player.closeInventory();
            }

            case "MenuGui"      -> menuGui.open(player);

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

            case "lvl1" -> { islandManager.setIslandLevel(player, 1); player.closeInventory(); }
            case "lvl2" -> { islandManager.setIslandLevel(player, 2); player.closeInventory(); }
            case "lvl3" -> { islandManager.setIslandLevel(player, 3); player.closeInventory(); }
            case "lvl4" -> { islandManager.setIslandLevel(player, 4); player.closeInventory(); }
            case "lvl5" -> { islandManager.setIslandLevel(player, 5); player.closeInventory(); }

            case "menuSizeLvl"  -> upgradeIslandGui.open(player);

            case "UpgradeToLevel" -> {
                String levelStr = container.get(new NamespacedKey(plugin, "target_level"), PersistentDataType.STRING);
                if (levelStr == null) return;
                try {
                    int targetLevel = Integer.parseInt(levelStr);
                    islandManager.upgradeIslandToLevel(player, targetLevel);
                    // Odświeżamy GUI po zakupie
                    upgradeIslandGui.open(player);
                } catch (NumberFormatException ex) {
                    plugin.getLogger().log(Level.WARNING, "[GuiListener] Nieprawidłowy target_level: " + levelStr, ex);
                }
            }

            case "MemberInfo" -> {
                if (targetUuidStr == null) return;
                OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(targetUuidStr));
                memberSettingsGui.open(player, target);
            }

            case "PromoteToLeader" -> {
                if (targetUuidStr == null) return;
                islandManager.transferOwnership(player, UUID.fromString(targetUuidStr));
                player.closeInventory();
            }

            case "SetCoLeader" -> {
                if (targetUuidStr == null) return;
                islandManager.setCoLeader(player, UUID.fromString(targetUuidStr));
                // Odśwież GUI po zmianie roli
                OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(targetUuidStr));
                memberSettingsGui.open(player, target);
            }

            case "KickMember" -> {
                if (targetUuidStr == null) return;
                islandManager.kickMember(player, UUID.fromString(targetUuidStr));
                membersGui.open(player);
            }

            case "LeaveIsland" -> {
                islandManager.leaveIsland(player);
                player.closeInventory();
            }

            case "OpenBans"  -> bannedPlayersGui.open(player);
            case "members"   -> membersGui.open(player);

            case "UnbanPlayer" -> {
                if (targetUuidStr == null) return;
                islandManager.unbanPlayer(player, UUID.fromString(targetUuidStr));
                bannedPlayersGui.open(player);
            }

            case "ChangeCategory" -> {
                String catName = container.get(new NamespacedKey(plugin, "category_name"), PersistentDataType.STRING);
                if (catName == null) return;
                try {
                    islandSettingsGui.open(player, ActionCategory.valueOf(catName));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING,
                            "[GuiListener] Nieznana kategoria: " + catName, ex);
                }
            }

            case "ToggleIslandSetting" -> {
                String actionName    = container.get(new NamespacedKey(plugin, "island_action"),    PersistentDataType.STRING);
                String currentCatName = container.get(new NamespacedKey(plugin, "current_category"), PersistentDataType.STRING);

                Island island = islandManager.getIsland(player.getUniqueId());
                if (island == null || actionName == null) return;

                try {
                    IslandAction targetAction = IslandAction.valueOf(actionName);

                    boolean newState = !island.canVisitorDo(targetAction);
                    island.setVisitorSetting(targetAction, newState);
                    islandManager.getStorage().saveIsland(island, IslandType.OVERWORLD);

                    ActionCategory cat = ActionCategory.GENERAL;
                    if (currentCatName != null) {
                        try {
                            cat = ActionCategory.valueOf(currentCatName);
                        } catch (IllegalArgumentException ex) {
                            plugin.getLogger().log(Level.WARNING,
                                    "[GuiListener] Nieznana kategoria: " + currentCatName, ex);
                        }
                    }

                    islandSettingsGui.open(player, cat);
                    player.sendMessage("§8» §7Zmieniono status §e" + targetAction.getDisplayName());

                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING,
                            "[GuiListener] Nieznana akcja wyspy: " + actionName, ex);
                }
            }

            case "OpenSettings" -> islandSettingsGui.open(player, ActionCategory.GENERAL);

            case "OpenIslandHub" -> islandHubGui.open(player);

            case "HubOpenSettings" -> {
                String typeStr = container.get(new NamespacedKey(plugin, "island_type"), PersistentDataType.STRING);
                if (typeStr == null) return;
                try {
                    IslandType type = IslandType.valueOf(typeStr);
                    if (type == IslandType.OVERWORLD) {
                        menuGui.open(player);
                    } else {
                        islandTypeSettingsGui.open(player, type);
                    }
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "[GuiListener] Nieznany typ wyspy: " + typeStr, ex);
                }
            }

            case "HubBuyIsland" -> {
                String typeStr = container.get(new NamespacedKey(plugin, "island_type"), PersistentDataType.STRING);
                if (typeStr == null) return;
                try {
                    IslandType type = IslandType.valueOf(typeStr);
                    player.closeInventory();
                    islandManager.createIsland(player, type);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "[GuiListener] Nieznany typ wyspy: " + typeStr, ex);
                }
            }

            case "IslandTypeTeleportHome" -> {
                String typeStr = container.get(new NamespacedKey(plugin, "island_type"), PersistentDataType.STRING);
                if (typeStr == null) return;
                try {
                    IslandType type = IslandType.valueOf(typeStr);
                    player.closeInventory();
                    islandManager.teleportHome(player, type);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "[GuiListener] Nieznany typ wyspy: " + typeStr, ex);
                }
            }

            case "IslandTypeOpenUpgrade" -> {
                String typeStr = container.get(new NamespacedKey(plugin, "island_type"), PersistentDataType.STRING);
                if (typeStr == null) return;
                try {
                    IslandType type = IslandType.valueOf(typeStr);
                    islandTypeUpgradeGui.open(player, type);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "[GuiListener] Nieznany typ wyspy: " + typeStr, ex);
                }
            }

            case "IslandTypeUpgradeToLevel" -> {
                String typeStr   = container.get(new NamespacedKey(plugin, "island_type"),  PersistentDataType.STRING);
                String levelStr  = container.get(new NamespacedKey(plugin, "target_level"), PersistentDataType.STRING);
                if (typeStr == null || levelStr == null) return;
                try {
                    IslandType type  = IslandType.valueOf(typeStr);
                    int targetLevel  = Integer.parseInt(levelStr);
                    islandManager.upgradeIslandToLevel(player, type, targetLevel);
                    islandTypeUpgradeGui.open(player, type);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "[GuiListener] Błąd IslandTypeUpgradeToLevel", ex);
                }
            }

            case "IslandTypeOpenSettings" -> {
                String typeStr = container.get(new NamespacedKey(plugin, "island_type"), PersistentDataType.STRING);
                if (typeStr == null) return;
                try {
                    IslandType type = IslandType.valueOf(typeStr);
                    islandTypeSettingsGui.open(player, type);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "[GuiListener] Nieznany typ wyspy: " + typeStr, ex);
                }
            }
        }
    }
}
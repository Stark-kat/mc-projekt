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
import stark.skyBlockTest2.crate.CrateAnimationGui;
import stark.skyBlockTest2.crate.CrateDefinition;
import stark.skyBlockTest2.crate.CrateManager;
import stark.skyBlockTest2.mail.MailManager;
import stark.skyBlockTest2.mail.gui.MailActionGui;
import stark.skyBlockTest2.mail.gui.MailComposeGui;
import stark.skyBlockTest2.mail.gui.MailGui;
import stark.skyBlockTest2.mail.gui.MailReadGui;
import stark.skyBlockTest2.shop.ShopManager;
import stark.skyBlockTest2.shop.auction.AuctionManager;
import stark.skyBlockTest2.shop.gui.*;
import stark.skyBlockTest2.teleport.TeleportManager;
import stark.skyBlockTest2.gui.menu.*;
import stark.skyBlockTest2.island.ActionCategory;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandAction;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.IslandType;
import stark.skyBlockTest2.island.generator.GeneratorManager;
import stark.skyBlockTest2.island.generator.GeneratorType;
import stark.skyBlockTest2.rank.Rank;
import stark.skyBlockTest2.settings.PlayerSettings;
import stark.skyBlockTest2.settings.PlayerSettingsGui;
import stark.skyBlockTest2.settings.PlayerSettingsManager;

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
    private final QuestsGui questsGui;
    private final QuestsMenuGui questsMenuGui;
    private final CrateAnimationGui crateAnimationGui;
    private final CrateManager crateManager;
    private final ShopHubGui shopHubGui;
    private final ShopCategoryGui shopCategoryGui;
    private final ShopItemsGui shopItemsGui;
    private final AuctionGui auctionGui;
    private final AuctionCreateGui auctionCreateGui;
    private final ShopManager shopManager;
    private final AuctionManager auctionManager;
    private final AuctionBidGui    auctionBidGui;
    private final MailManager mailManager;
    private final MailGui mailGui;
    private final MailComposeGui mailComposeGui;
    private final MailReadGui mailReadGui;
    private final MailActionGui mailActionGui;
    private final PlayerSettingsGui playerSettingsGui;
    private final PlayerSettingsManager playerSettingsManager;
    private final GeneratorUpgradeGui generatorUpgradeGui;
    private final GeneratorManager generatorManager;


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
                       IslandTypeUpgradeGui islandTypeUpgradeGui,
                       QuestsGui questsGui,
                       QuestsMenuGui questsMenuGui,
                       CrateAnimationGui crateAnimationGui,
                       CrateManager crateManager,
                       ShopHubGui shopHubGui,
                       ShopCategoryGui shopCategoryGui,
                       ShopItemsGui shopItemsGui,
                       AuctionGui auctionGui,
                       AuctionCreateGui auctionCreateGui,
                       ShopManager shopManager,
                       AuctionManager auctionManager,
                       AuctionBidGui auctionBidGui,
                       MailComposeGui mailComposeGui,
                       MailGui mailGui,
                       MailReadGui mailReadGui,
                       MailManager mailManager,
                       MailActionGui mailActionGui,
                       PlayerSettingsGui playerSettingsGui,
                       PlayerSettingsManager playerSettingsManager,
                       GeneratorUpgradeGui generatorUpgradeGui,
                       GeneratorManager generatorManager) {

        this.teleportManager = teleportManager;
        this.islandManager = islandManager;
        this.plugin = plugin;
        this.menuGui = menuGui;
        this.upgradeIslandGui = upgradeIslandGui;
        this.membersGui = membersGui;
        this.memberSettingsGui = memberSettingsGui;
        this.islandSettingsGui = islandSettingsGui;
        this.bannedPlayersGui = bannedPlayersGui;
        this.islandHubGui = islandHubGui;
        this.islandTypeSettingsGui = islandTypeSettingsGui;
        this.islandTypeUpgradeGui = islandTypeUpgradeGui;
        this.questsGui = questsGui;
        this.questsMenuGui = questsMenuGui;
        this.crateAnimationGui = crateAnimationGui;
        this.crateManager = crateManager;
        this.shopHubGui = shopHubGui;
        this.shopCategoryGui = shopCategoryGui;
        this.shopItemsGui = shopItemsGui;
        this.auctionGui = auctionGui;
        this.auctionCreateGui = auctionCreateGui;
        this.shopManager = shopManager;
        this.auctionManager = auctionManager;
        this.auctionBidGui = auctionBidGui;
        this.mailGui = mailGui;
        this.mailComposeGui = mailComposeGui;
        this.mailReadGui = mailReadGui;
        this.mailManager = mailManager;
        this.mailActionGui = mailActionGui;
        this.playerSettingsGui = playerSettingsGui;
        this.playerSettingsManager = playerSettingsManager;
        this.generatorUpgradeGui = generatorUpgradeGui;
        this.generatorManager = generatorManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Inventory topInventory = e.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof MenuHolder)) return;

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

        String action = container.get(actionKey, PersistentDataType.STRING);
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

            case "MenuGui" -> menuGui.open(player);

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

            case "OpenBans" -> bannedPlayersGui.open(player);
            case "members" -> membersGui.open(player);

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
                String actionName = container.get(new NamespacedKey(plugin, "island_action"), PersistentDataType.STRING);
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
                    islandTypeSettingsGui.open(player, type);  // tak samo jak inne typy
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
                String typeStr = container.get(new NamespacedKey(plugin, "island_type"), PersistentDataType.STRING);
                String levelStr = container.get(new NamespacedKey(plugin, "target_level"), PersistentDataType.STRING);
                if (typeStr == null || levelStr == null) return;
                try {
                    IslandType type = IslandType.valueOf(typeStr);
                    int targetLevel = Integer.parseInt(levelStr);
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

            // Otwiera pośrednie GUI z 3 kafelkami
            case "OpenQuestsMenu" -> questsMenuGui.open(player);

            // Otwiera konkretne GUI (dzienne/tygodniowe/osiągnięcia)
            case "QuestsOpen" -> {
                String viewName = container.get(new NamespacedKey(plugin, "view"), PersistentDataType.STRING);
                if (viewName != null) {
                    try {
                        QuestsGui.View view = QuestsGui.View.valueOf(viewName);
                        questsGui.open(player, view);
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("[GuiListener] Nieznany widok: " + viewName);
                    }
                }
            }

            case "QuestsCategoryOpen" -> {
                NamespacedKey catKey = new NamespacedKey(plugin, "category");
                String category = meta.getPersistentDataContainer()
                        .get(catKey, PersistentDataType.STRING);
                if (category != null) questsGui.openAchievementsByCategory(player, category);
            }
            case "OpenAchievementCategories" -> questsGui.openAchievements(player);
            case "open_crate" -> {
                String crateId = container.get(
                        new NamespacedKey(plugin, "crate_id"), PersistentDataType.STRING);
                CrateDefinition crate = crateManager.getCrate(crateId);
                if (crate != null) crateAnimationGui.open(player, crate);
            }
            case "OpenShopHub" -> shopHubGui.open(player);

            case "OpenShopCategories" -> shopCategoryGui.open(player);

            case "OpenShopItems" -> {
                String catId = container.get(new NamespacedKey(plugin, "category_id"), PersistentDataType.STRING);
                if (catId != null) shopItemsGui.open(player, catId, 0);
            }

            case "ShopPage" -> {
                String catId = container.get(new NamespacedKey(plugin, "category_id"), PersistentDataType.STRING);
                String pageStr = container.get(new NamespacedKey(plugin, "page"), PersistentDataType.STRING);
                if (catId != null && pageStr != null) {
                    try { shopItemsGui.open(player, catId, Integer.parseInt(pageStr)); }
                    catch (NumberFormatException ignored) {}
                }
            }

            case "ShopBuySell" -> {
                String shopItemId = container.get(new NamespacedKey(plugin, "shop_item_id"), PersistentDataType.STRING);
                if (shopItemId != null) {
                    shopItemsGui.handleTransaction(player, shopItemId, e.isRightClick(), e.isShiftClick());
                }
            }

            case "OpenAuctions" -> auctionGui.open(player, 0);

            case "AuctionTab" -> {
                String tabStr  = container.get(new NamespacedKey(plugin, "auction_tab"), PersistentDataType.STRING);
                if (tabStr == null) return;
                try {
                    AuctionGui.Tab tab = AuctionGui.Tab.valueOf(tabStr);
                    auctionGui.openTab(player, tab, 0);
                } catch (IllegalArgumentException ignored) {}
            }

            case "AuctionPage" -> {
                String tabStr  = container.get(new NamespacedKey(plugin, "auction_tab"), PersistentDataType.STRING);
                String pageStr = container.get(new NamespacedKey(plugin, "page"), PersistentDataType.STRING);
                AuctionGui.Tab tab = AuctionGui.Tab.ALL;
                if (tabStr != null) {
                    try { tab = AuctionGui.Tab.valueOf(tabStr); } catch (IllegalArgumentException ignored) {}
                }
                int page = 0;
                if (pageStr != null) {
                    try { page = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
                }
                auctionGui.openTab(player, tab, page);
            }

            case "OpenAuctionCreate" -> auctionCreateGui.open(player);

            case "AuctionSetType" -> {
                String typeStr = container.get(new NamespacedKey(plugin, "auction_type"), PersistentDataType.STRING);
                if (typeStr != null) {
                    try { auctionCreateGui.setType(player, stark.skyBlockTest2.shop.auction.AuctionListing.Type.valueOf(typeStr)); }
                    catch (IllegalArgumentException ignored) {}
                }
            }

            case "AuctionSetDuration" -> {
                String durStr = container.get(new NamespacedKey(plugin, "auction_duration"), PersistentDataType.STRING);
                if (durStr != null) {
                    try { auctionCreateGui.setDuration(player, Integer.parseInt(durStr)); }
                    catch (NumberFormatException ignored) {}
                }
            }

            case "AuctionAdjustPrice" -> {
                String dir = container.get(new NamespacedKey(plugin, "adjust"), PersistentDataType.STRING);
                if (dir != null) {
                    auctionCreateGui.adjustPrice(player, dir, e.isShiftClick(), e.isRightClick());
                }
            }

            case "AuctionTypePrice" -> {
                // TODO: implementacja wpisywania ceny przez chat
                player.sendMessage("§7Wpisz cenę na czacie (funkcja wkrótce).");
            }

            case "AuctionConfirm" -> auctionCreateGui.confirm(player);

            case "AuctionBuyNow" -> {
                String auctionId = container.get(new NamespacedKey(plugin, "auction_id"), PersistentDataType.STRING);
                if (auctionId == null) return;
                String err = auctionManager.buyNow(player, auctionId);
                if (err != null) player.sendMessage(err);
                else auctionGui.open(player, 0);
            }

            case "AuctionOpenBid" -> {
                String auctionId = container.get(new NamespacedKey(plugin, "auction_id"), PersistentDataType.STRING);
                if (auctionId != null) auctionBidGui.open(player, auctionId);
            }

            case "BidAdjust" -> {
                String listingId = container.get(new NamespacedKey(plugin, "bid_listing_id"), PersistentDataType.STRING);
                String adjustStr = container.get(new NamespacedKey(plugin, "adjust"), PersistentDataType.STRING);
                if (listingId != null && adjustStr != null) {
                    auctionBidGui.adjustBid(player, listingId, adjustStr);
                }
            }

            case "BidConfirm" -> {
                String listingId = container.get(new NamespacedKey(plugin, "bid_listing_id"), PersistentDataType.STRING);
                if (listingId != null) auctionBidGui.confirmBid(player, listingId);
            }

            case "AuctionCancel" -> {
                String auctionId = container.get(new NamespacedKey(plugin, "auction_id"), PersistentDataType.STRING);
                if (auctionId == null) return;
                String err = auctionManager.cancelListing(player, auctionId);
                if (err != null) player.sendMessage(err);
                else auctionGui.openMyListings(player, 0);
            }

            case "ShopSellAll" -> {
                double total = shopItemsGui.sellAll(player);
                if (total > 0) {
                    player.sendMessage("§a§lSprzedano wszystko! §r§7Otrzymano §e"
                            + stark.skyBlockTest2.util.PriceFormat.format(total));
                    shopCategoryGui.open(player); // odśwież GUI kategorii
                } else {
                    player.sendMessage("§cNic do sprzedania w sklepie!");
                }
            }

            case "AuctionCycleType"  -> auctionGui.cycleTypeFilter(player);
            case "AuctionCyclePrice" -> auctionGui.cyclePriceSort(player);
            case "AuctionCycleTime"  -> auctionGui.cycleTimeSort(player);

            case "MailPage" -> {
                String pageStr = container.get(new NamespacedKey(plugin, "page"), PersistentDataType.STRING);
                mailGui.open(player, pageStr != null ? Integer.parseInt(pageStr) : 0);
            }

            case "MailRead" -> {
                String id = container.get(new NamespacedKey(plugin, "mail_id"), PersistentDataType.STRING);
                if (id != null) {
                    mailManager.markRead(player.getUniqueId(), id);
                    mailActionGui.open(player, id);
                }
            }

            case "MailReadBook" -> {
                String id = container.get(new NamespacedKey(plugin, "mail_id"), PersistentDataType.STRING);
                if (id != null) mailReadGui.openBook(player, id);
            }

            case "MailBack" -> mailGui.open(player, 0);

            case "MailClaim" -> {
                String id = container.get(new NamespacedKey(plugin, "mail_id"), PersistentDataType.STRING);
                if (id == null) return;
                String err = mailManager.claimRewards(player, id);
                if (err != null) player.sendMessage(err);
                else mailActionGui.open(player, id);
            }

            case "MailDelete" -> {
                String id = container.get(new NamespacedKey(plugin, "mail_id"), PersistentDataType.STRING);
                if (id == null) return;
                String err = mailManager.deleteMessage(player, id);
                if (err != null) player.sendMessage(err);
                else mailGui.open(player, 0);
            }

            case "MailCompose"          -> mailComposeGui.open(player);
            case "MailComposeRecipient" -> mailComposeGui.promptRecipient(player);
            case "MailComposeSubject"   -> mailComposeGui.promptSubject(player);
            case "MailComposeItem"      -> mailComposeGui.toggleItem(player);
            case "MailComposeMoney"     -> mailComposeGui.promptMoney(player);
            case "MailComposeSend"      -> mailComposeGui.send(player);
            case "MailComposeCancel"    -> mailComposeGui.cancel(player);
            case "MailComposeMessage" -> mailComposeGui.promptMessage(player);
            case "MailComposeNoop"      -> {}

            case "OpenGeneratorUpgrade" -> {
                String typeStr = container.get(new NamespacedKey(plugin, "island_type"), PersistentDataType.STRING);
                if (typeStr == null) return;
                try {
                    IslandType type = IslandType.valueOf(typeStr);
                    generatorUpgradeGui.open(player, type);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "[GuiListener] Nieznany typ wyspy: " + typeStr, ex);
                }
            }

            case "UpgradeGenerator" -> {
                String typeStr = container.get(new NamespacedKey(plugin, "island_type"), PersistentDataType.STRING);
                if (typeStr == null) return;
                try {
                    IslandType islandType = IslandType.valueOf(typeStr);
                    Island island = islandManager.getIsland(player.getUniqueId(), islandType);
                    if (island == null) return;
                    GeneratorType genType = GeneratorType.fromWorld(islandType.worldName);
                    if (genType == null) return;
                    generatorManager.upgrade(player, island, genType);
                    generatorUpgradeGui.open(player, islandType);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "[GuiListener] Błąd UpgradeGenerator", ex);
                }
            }

            case "OpenPlayerSettings" -> playerSettingsGui.open(player);

            case "PlayerSettingsToggle" -> {
                String setting = container.get(new NamespacedKey(plugin, "setting"), PersistentDataType.STRING);
                if (setting == null) return;
                PlayerSettings s = playerSettingsManager.getSettings(player.getUniqueId());
                switch (setting) {
                    case "borderVisible" -> {
                        if (plugin.getRankManager().getRank(player).getWeight() < Rank.VIP.getWeight()) {
                            player.sendMessage("§cTa opcja dostępna jest od rangi §6VIP§c!");
                            return;
                        }
                        s.setBorderVisible(!s.isBorderVisible());
                    }
                    case "particlesEnabled"    -> s.setParticlesEnabled(!s.isParticlesEnabled());
                    case "acceptTpa"          -> s.setAcceptTpa(!s.isAcceptTpa());
                    case "acceptMsg"          -> s.setAcceptMsg(!s.isAcceptMsg());
                    case "sidebarEnabled"     -> s.setSidebarEnabled(!s.isSidebarEnabled());
                    case "sidebarIslandLevel" -> s.setSidebarIslandLevel(!s.isSidebarIslandLevel());
                    case "sidebarIslandXp"    -> s.setSidebarIslandXp(!s.isSidebarIslandXp());
                    case "sidebarBalance"     -> s.setSidebarBalance(!s.isSidebarBalance());
                    case "sidebarDailyQuests" -> s.setSidebarDailyQuests(!s.isSidebarDailyQuests());
                    case "sidebarWeeklyQuests"-> s.setSidebarWeeklyQuests(!s.isSidebarWeeklyQuests());
                    case "xpBossbarEnabled"   -> s.setXpBossbarEnabled(!s.isXpBossbarEnabled());
                    case "sidebarIslandMembers" -> s.setSidebarIslandMembers(!s.isSidebarIslandMembers());
                    case "directDrop" -> {
                        if (plugin.getRankManager().getRank(player).getWeight() < Rank.VIP.getWeight()) {
                            player.sendMessage("§cTa opcja dostępna jest od rangi §6VIP§c!");
                            return;
                        }
                        s.setDirectDrop(!s.isDirectDrop());
                    }
                }
                playerSettingsManager.saveSettings(player.getUniqueId(), s);
                if ("borderVisible".equals(setting)) {
                    plugin.getBorderManager().updateBorder(player);
                }
                playerSettingsGui.open(player);
            }

            case "MailReply" -> {
                String id       = container.get(new NamespacedKey(plugin, "mail_id"), PersistentDataType.STRING);
                String uuidStr  = container.get(new NamespacedKey(plugin, "reply_to_uuid"), PersistentDataType.STRING);
                if (id == null || uuidStr == null) return;
                UUID replyUuid = UUID.fromString(uuidStr);
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(replyUuid);
                String name = target.getName() != null ? target.getName() : "Gracz";
                mailComposeGui.openReply(player, id, replyUuid, name);
            }
        }
    }
}

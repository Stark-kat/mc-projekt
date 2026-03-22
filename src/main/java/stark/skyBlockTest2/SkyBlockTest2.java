package stark.skyBlockTest2;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import stark.skyBlockTest2.border.BorderListener;
import stark.skyBlockTest2.border.BorderManager;
import stark.skyBlockTest2.crate.CrateAnimationGui;
import stark.skyBlockTest2.crate.CrateManager;
import stark.skyBlockTest2.database.DatabaseManager;
import stark.skyBlockTest2.economy.EconomyManager;
import stark.skyBlockTest2.economy.command.EcoCommand;
import stark.skyBlockTest2.economy.command.MoneyCommand;
import stark.skyBlockTest2.economy.command.PayCommand;
import stark.skyBlockTest2.economy.listener.EconomyListener;
import stark.skyBlockTest2.gui.listener.GuiListener;
import stark.skyBlockTest2.gui.menu.*;
import stark.skyBlockTest2.island.IslandBossBarManager;
import stark.skyBlockTest2.sidebar.SidebarManager;
import stark.skyBlockTest2.island.SchematicManager;
import stark.skyBlockTest2.island.command.IslandCommand;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.command.IslandTabCompleter;
import stark.skyBlockTest2.island.generator.GeneratorConfig;
import stark.skyBlockTest2.island.generator.GeneratorManager;
import stark.skyBlockTest2.island.generator.listener.BlockFormGeneratorListener;
import stark.skyBlockTest2.island.generator.listener.MachineGeneratorListener;
import stark.skyBlockTest2.island.listener.IslandProtectionListener;
import stark.skyBlockTest2.item.CustomItemRegistry;
import stark.skyBlockTest2.mail.MailManager;
import stark.skyBlockTest2.mail.command.MailCommand;
import stark.skyBlockTest2.mail.gui.MailActionGui;
import stark.skyBlockTest2.mail.gui.MailComposeGui;
import stark.skyBlockTest2.mail.gui.MailGui;
import stark.skyBlockTest2.mail.gui.MailReadGui;
import stark.skyBlockTest2.mail.listener.MailListener;
import stark.skyBlockTest2.quest.QuestManager;
import stark.skyBlockTest2.rank.RankCommand;
import stark.skyBlockTest2.rank.RankListener;
import stark.skyBlockTest2.rank.RankManager;
import stark.skyBlockTest2.shop.ShopManager;
import stark.skyBlockTest2.shop.auction.AuctionManager;
import stark.skyBlockTest2.shop.gui.*;
import stark.skyBlockTest2.spawn.SetSpawnCommand;
import stark.skyBlockTest2.spawn.SpawnCommand;
import stark.skyBlockTest2.teleport.TeleportListener;
import stark.skyBlockTest2.teleport.TeleportManager;
import stark.skyBlockTest2.teleport.TpaCommand;
import stark.skyBlockTest2.teleport.TpaManager;
import stark.skyBlockTest2.settings.PlayerSettingsGui;
import stark.skyBlockTest2.settings.PlayerSettingsManager;
import stark.skyBlockTest2.util.ChatInputManager;
import stark.skyBlockTest2.world.WorldManager;
import stark.skyBlockTest2.util.CounterManager;
import stark.skyBlockTest2.util.JoinMessage;

import java.sql.SQLException;

public class SkyBlockTest2 extends JavaPlugin {

    private static SkyBlockTest2 instance;
    private WorldManager worldManager;
    private IslandManager islandManager;
    private TeleportManager teleportManager;
    private SchematicManager schematicManager;
    private EconomyManager economyManager;
    private QuestManager questManager;
    private ShopManager shopManager;
    private AuctionManager auctionManager;
    private DatabaseManager databaseManager;
    private BorderManager borderManager;
    private CrateManager crateManager;
    private GeneratorManager generatorManager;
    private RankManager rankManager;
    private IslandBossBarManager islandBossBarManager;
    private SidebarManager sidebarManager;
    private TpaManager tpaManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        databaseManager.loadAllPendingRewards();

        try {
            rankManager = new RankManager(this);
            // Przy reload gracze są już online — PlayerJoinEvent nie odpali się ponownie,
            // więc ładujemy rangi ręcznie dla wszystkich aktualnie zalogowanych graczy.
            getServer().getOnlinePlayers().forEach(rankManager::onPlayerJoin);
            getServer().getPluginManager().registerEvents(new RankListener(rankManager), this);
            getCommand("rank").setExecutor(new RankCommand(rankManager));
            getCommand("rank").setTabCompleter(new RankCommand(rankManager));
        } catch (SQLException e) {
            getLogger().severe("Błąd inicjalizacji RankManager: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }

        worldManager = new WorldManager(this);
        worldManager.createWorlds();

        schematicManager = new SchematicManager(this);
        teleportManager = new TeleportManager(this, rankManager);
        tpaManager = new TpaManager(this, teleportManager);
        CounterManager counterManager = new CounterManager(this);

        islandManager = new IslandManager(this, teleportManager);
        borderManager = new BorderManager(islandManager);
        islandManager.setBorderManager(borderManager);

        // Własna ekonomia — rejestruje się w Vault jako provider
        economyManager = new EconomyManager(this);
        economyManager.setup();
        islandManager.setEconomyManager(economyManager);
        getServer().getPluginManager().registerEvents(new EconomyListener(economyManager), this);

        getCommand("money").setExecutor(new MoneyCommand(economyManager));
        getCommand("pay").setExecutor(new PayCommand(economyManager));
        EcoCommand ecoCommand = new EcoCommand(economyManager);
        getCommand("eco").setExecutor(ecoCommand);
        getCommand("eco").setTabCompleter(ecoCommand);

        // Generator — wymaga ekonomii, więc inicjalizujemy po jej setupie
        GeneratorConfig generatorConfig = new GeneratorConfig(this);
        generatorManager = new GeneratorManager(
                this, generatorConfig, islandManager, economyManager.getEconomy(),
                databaseManager.getConnection()
        );

        CustomItemRegistry.registerAll();

        MenuGui menuGui = new MenuGui();
        UpgradeIslandGui upgradeIslandGui = new UpgradeIslandGui(islandManager, economyManager);
        MemberSettingsGui memberSettingsGui = new MemberSettingsGui(islandManager);
        MembersGui membersGui = new MembersGui(islandManager);
        IslandSettingsGui islandSettingsGui = new IslandSettingsGui(islandManager);
        CreateIslandGui createIslandGui = new CreateIslandGui();
        BannedPlayersGui bannedPlayersGui = new BannedPlayersGui(islandManager);
        IslandHubGui islandHubGui = new IslandHubGui(islandManager, economyManager);
        IslandTypeSettingsGui islandTypeSettingsGui = new IslandTypeSettingsGui(islandManager);
        IslandTypeUpgradeGui islandTypeUpgradeGui = new IslandTypeUpgradeGui(islandManager, economyManager);
        crateManager = new CrateManager(this);
        questManager = new QuestManager(this, islandManager);
        islandBossBarManager = new IslandBossBarManager(this, questManager);
        questManager.setBossBarManager(islandBossBarManager);
        questManager.setEconomyManager(economyManager);
        QuestsGui questsGui = new QuestsGui(islandManager, questManager);
        QuestsMenuGui questsMenuGui = new QuestsMenuGui(islandManager, questManager);
        CrateAnimationGui crateAnimationGui = new CrateAnimationGui(this, databaseManager);
        shopManager    = new ShopManager(this);
        auctionManager = new AuctionManager(this, economyManager);
        PlayerSettingsManager playerSettingsManager = new PlayerSettingsManager(databaseManager);
        tpaManager.setSettingsManager(playerSettingsManager);
        borderManager.setPlayerSettingsManager(playerSettingsManager);
        borderManager.setRankManager(rankManager);
        islandBossBarManager.setSettingsManager(playerSettingsManager);
        PlayerSettingsGui playerSettingsGui = new PlayerSettingsGui(playerSettingsManager, rankManager);
        sidebarManager = new SidebarManager(this, islandManager, questManager, economyManager, playerSettingsManager);
        getServer().getOnlinePlayers().forEach(sidebarManager::initPlayer);
        GeneratorUpgradeGui generatorUpgradeGui = new GeneratorUpgradeGui(islandManager, generatorManager, economyManager);
        ChatInputManager chatInputManager = new ChatInputManager(this);
        MailManager mailManager = new MailManager(this, economyManager);
        auctionManager.setMailManager(mailManager);
        auctionManager.setQuestManager(questManager);
        islandManager.setQuestManager(questManager);
        generatorManager.setQuestManager(questManager);

        ShopHubGui shopHubGui       = new ShopHubGui(shopManager);
        ShopCategoryGui shopCategoryGui  = new ShopCategoryGui(shopManager);
        ShopItemsGui shopItemsGui     = new ShopItemsGui(this, shopManager, economyManager);
        shopItemsGui.setQuestManager(questManager);
        AuctionGui auctionGui       = new AuctionGui(this, auctionManager);
        AuctionCreateGui auctionCreateGui = new AuctionCreateGui(auctionManager);
        AuctionBidGui    auctionBidGui    = new AuctionBidGui(auctionManager, auctionGui);
        MailGui mailGui = new MailGui(mailManager);
        MailActionGui mailActionGui = new MailActionGui(mailManager);
        MailReadGui mailReadGui = new MailReadGui(this, mailManager);
        MailComposeGui mailComposeGui = new MailComposeGui(this, mailManager, chatInputManager, mailGui);

        IslandCommand islandCmd = new IslandCommand(this, islandManager, menuGui, createIslandGui);

        PluginCommand islandCommand = getCommand("island");
        PluginCommand spawnCommand = getCommand("spawn");
        PluginCommand setSpawnCommand = getCommand("setspawn");
        PluginCommand auctionCommand = getCommand("aukcja");
        getCommand("poczta").setExecutor(new MailCommand(mailGui));

        if (islandCommand != null) {
            islandCommand.setExecutor(islandCmd);
            islandCommand.setTabCompleter(new IslandTabCompleter());
        } else {
            getLogger().severe("Komenda 'island' nie znaleziona w plugin.yml!");
        }

        if (spawnCommand != null) {
            spawnCommand.setExecutor(new SpawnCommand(teleportManager));
        } else {
            getLogger().severe("Komenda 'spawn' nie znaleziona w plugin.yml!");
        }

        if (setSpawnCommand != null) {
            setSpawnCommand.setExecutor(new SetSpawnCommand(this, teleportManager));
        } else {
            getLogger().severe("Komenda 'setspawn' nie znaleziona w plugin.yml!");
        }

        if (auctionCommand != null) {
            auctionCommand.setExecutor(new stark.skyBlockTest2.shop.command.AuctionCommand(auctionCreateGui));
        }

        TpaCommand tpaCommand = new TpaCommand(tpaManager);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpa").setTabCompleter(tpaCommand);
        getCommand("tpaaccept").setExecutor(tpaCommand);
        getCommand("tpadeny").setExecutor(tpaCommand);

        getServer().getPluginManager().registerEvents(new IslandProtectionListener(islandManager), this);
        getServer().getPluginManager().registerEvents(new BorderListener(borderManager, this), this);
        getServer().getPluginManager().registerEvents(new JoinMessage(counterManager), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(teleportManager), this);
        getServer().getPluginManager().registerEvents(new BlockFormGeneratorListener(islandManager, generatorManager), this);
        getServer().getPluginManager().registerEvents(new MachineGeneratorListener(islandManager, generatorManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(teleportManager, islandManager, this, menuGui,
                upgradeIslandGui, membersGui, memberSettingsGui, islandSettingsGui, bannedPlayersGui, islandHubGui,
                islandTypeSettingsGui, islandTypeUpgradeGui, questsGui, questsMenuGui, crateAnimationGui, crateManager,
                shopHubGui, shopCategoryGui,shopItemsGui, auctionGui, auctionCreateGui, shopManager, auctionManager, auctionBidGui,
                mailComposeGui, mailGui, mailReadGui, mailManager, mailActionGui,
                playerSettingsGui, playerSettingsManager,
                generatorUpgradeGui, generatorManager), this);
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                sidebarManager.initPlayer(e.getPlayer());
            }
            @org.bukkit.event.EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
                playerSettingsManager.unloadPlayer(e.getPlayer().getUniqueId());
                islandBossBarManager.remove(e.getPlayer().getUniqueId());
                sidebarManager.removePlayer(e.getPlayer().getUniqueId());
                tpaManager.onPlayerQuit(e.getPlayer().getUniqueId());
            }
        }, this);
        getServer().getPluginManager().registerEvents(new stark.skyBlockTest2.quest.listener.QuestListener(questManager, this), this);
        getServer().getPluginManager().registerEvents(new stark.skyBlockTest2.item.CustomItemListener(this, islandManager, crateManager, crateAnimationGui), this);
        getServer().getPluginManager().registerEvents(new MailListener(mailManager), this);
        getServer().getPluginManager().registerEvents(new stark.skyBlockTest2.settings.DirectDropListener(playerSettingsManager, rankManager), this);
        getServer().getPluginManager().registerEvents(chatInputManager, this);

        islandManager.performStartupScan();
        generatorManager.loadAllMachines();

        getLogger().info("SkyBlockTest2 enabled!");
    }

    @Override
    public void onDisable() {
        if (teleportManager != null) {
            teleportManager.cancelAll();
        }
        if (generatorManager != null) {
            generatorManager.shutdown();
        }
        if (islandBossBarManager != null) {
            islandBossBarManager.removeAll();
        }
        if (databaseManager != null) databaseManager.disconnect();
    }

    public static SkyBlockTest2 getInstance() {
        return instance;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }

    public QuestManager getQuestManager() { return questManager; }

    public ShopManager    getShopManager()    { return shopManager; }

    public AuctionManager getAuctionManager() { return auctionManager; }
    public BorderManager  getBorderManager()  { return borderManager; }
    public CrateManager   getCrateManager()   { return crateManager; }
    public GeneratorManager getGeneratorManager() { return generatorManager; }
    public RankManager getRankManager() { return rankManager; }
}
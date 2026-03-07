package stark.skyBlockTest2;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import stark.skyBlockTest2.border.BorderListener;
import stark.skyBlockTest2.border.BorderManager;
import stark.skyBlockTest2.economy.EconomyManager;
import stark.skyBlockTest2.gui.listener.GuiListener;
import stark.skyBlockTest2.gui.menu.*;
import stark.skyBlockTest2.island.SchematicManager;
import stark.skyBlockTest2.island.command.IslandCommand;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.command.IslandTabCompleter;
import stark.skyBlockTest2.island.listener.IslandProtectionListener;
import stark.skyBlockTest2.spawn.SetSpawnCommand;
import stark.skyBlockTest2.spawn.SpawnCommand;
import stark.skyBlockTest2.teleport.TeleportListener;
import stark.skyBlockTest2.teleport.TeleportManager;
import stark.skyBlockTest2.world.WorldManager;
import stark.skyBlockTest2.util.CounterMenager;
import stark.skyBlockTest2.util.JoinMessage;

public class SkyBlockTest2 extends JavaPlugin {

    private static SkyBlockTest2 instance;
    private WorldManager worldManager;
    private IslandManager islandManager;
    private TeleportManager teleportManager;
    private SchematicManager schematicManager;
    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("islands.yml", false);

        worldManager = new WorldManager(this);
        worldManager.createWorlds();

        schematicManager = new SchematicManager(this);
        teleportManager = new TeleportManager(this);
        CounterMenager counterMenager = new CounterMenager(this);

        islandManager = new IslandManager(this, teleportManager);
        BorderManager borderManager = new BorderManager(islandManager);
        islandManager.setBorderManager(borderManager);

        // Ekonomia — setup po załadowaniu wszystkich pluginów
        economyManager = new EconomyManager(this);
        if (economyManager.setup()) {
            islandManager.setEconomyManager(economyManager);
        }

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

        IslandCommand islandCmd = new IslandCommand(islandManager, menuGui, createIslandGui);

        PluginCommand islandCommand = getCommand("island");
        PluginCommand spawnCommand = getCommand("spawn");
        PluginCommand setSpawnCommand = getCommand("setspawn");

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

        getServer().getPluginManager().registerEvents(new IslandProtectionListener(islandManager), this);
        getServer().getPluginManager().registerEvents(new BorderListener(borderManager, this), this);
        getServer().getPluginManager().registerEvents(new JoinMessage(counterMenager), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(teleportManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(teleportManager, islandManager, this, menuGui, upgradeIslandGui, membersGui, memberSettingsGui, islandSettingsGui, bannedPlayersGui, islandHubGui, islandTypeSettingsGui, islandTypeUpgradeGui), this);

        islandManager.performStartupScan();
        getLogger().info("SkyBlockTest2 enabled!");
    }

    @Override
    public void onDisable() {
        if (teleportManager != null) {
            teleportManager.cancelAll();
        }
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
}
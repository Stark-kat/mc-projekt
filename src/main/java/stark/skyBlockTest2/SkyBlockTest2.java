package stark.skyBlockTest2;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import stark.skyBlockTest2.border.BorderListener;
import stark.skyBlockTest2.border.BorderManager;
import stark.skyBlockTest2.gui.listener.GuiListener;
import stark.skyBlockTest2.gui.menu.*;
import stark.skyBlockTest2.island.SchematicManager;
import stark.skyBlockTest2.island.command.IslandCommand;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.command.IslandLvlCommand;
import stark.skyBlockTest2.island.command.IslandTabCompleter;
import stark.skyBlockTest2.island.listener.IslandProtectionListener;
import stark.skyBlockTest2.Spawn.SetSpawnCommand;
import stark.skyBlockTest2.Spawn.SpawnCommand;
import stark.skyBlockTest2.Spawn.TeleportListener;
import stark.skyBlockTest2.Spawn.TeleportManager;
import stark.skyBlockTest2.World.WorldManager;
import stark.skyBlockTest2.util.CounterMenager;
import stark.skyBlockTest2.util.JoinMessage;

public class SkyBlockTest2 extends JavaPlugin {

    private static SkyBlockTest2 instance;
    private WorldManager worldManager;
    private IslandManager islandManager;
    private TeleportManager teleportManager;
    private MenuGui menuGui;
    private UpgradeIslandGui upgradeIslandGui;
    private MembersGui membersGui;
    private MemberSettingsGui memberSettingsGui;
    private IslandSettingsGui islandSettingsGui;
    private BorderManager borderManager;
    private SchematicManager schematicManager;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();
        saveResource("islands.yml", false);

        worldManager = new WorldManager(this);
        worldManager.createWorld();
        World islandWorld = Bukkit.getWorld("world_skyblock");
        this.schematicManager = new SchematicManager(this);

        teleportManager = new TeleportManager(this);
        CounterMenager counterMenager = new CounterMenager(this);

        this.islandManager = new IslandManager(this, teleportManager);
        this.borderManager = new BorderManager(this.islandManager, islandWorld);
        this.islandManager.setBorderManager(this.borderManager);

        menuGui = new MenuGui();
        upgradeIslandGui = new UpgradeIslandGui();
        memberSettingsGui = new MemberSettingsGui();
        membersGui = new MembersGui(islandManager);
        islandSettingsGui = new IslandSettingsGui(islandManager);

        MenuGui menuGui = new MenuGui();
        CreateIslandGui createIslandGui = new CreateIslandGui();

        IslandCommand islandCmd = new IslandCommand(islandManager, menuGui, createIslandGui);


        getCommand("island").setExecutor(new IslandCommand(islandManager, menuGui, createIslandGui));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this, teleportManager));
        getCommand("spawn").setExecutor(new SpawnCommand(teleportManager));
        //getCommand("issetlvl").setExecutor(new IslandLvlCommand());
        getCommand("island").setExecutor(islandCmd);
        getCommand("island").setTabCompleter(new IslandTabCompleter());

        //getCommand("menu").setExecutor(new MenuCommand(menuGui, islandManager, createIslandGui));

        getServer().getPluginManager().registerEvents(new IslandProtectionListener(islandManager),this);
        getServer().getPluginManager().registerEvents(new BorderListener(borderManager, this), this);
        getServer().getPluginManager().registerEvents(new JoinMessage(counterMenager), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(teleportManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(teleportManager, islandManager, this, menuGui, upgradeIslandGui, membersGui, memberSettingsGui, islandSettingsGui), this);

        islandManager.performStartupScan();
        getLogger().info("SkyBlockTest2 enabled!");
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

    @Override
    public void onDisable() {
        if (teleportManager != null) {
            teleportManager.cancelAll();
        }
    }
}

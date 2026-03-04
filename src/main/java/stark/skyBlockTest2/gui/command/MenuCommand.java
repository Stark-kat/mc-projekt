package stark.skyBlockTest2.gui.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import stark.skyBlockTest2.gui.menu.CreateIslandGui;
import stark.skyBlockTest2.gui.menu.MenuGui;
import stark.skyBlockTest2.island.IslandManager;


public class MenuCommand implements CommandExecutor {

    private final MenuGui menuGui;
    private final IslandManager islandManager;
    private final CreateIslandGui createIslandGui;

    public MenuCommand(MenuGui menuGui, IslandManager islandManager, CreateIslandGui createIslandGui) {
        this.menuGui = menuGui;
        this.createIslandGui = createIslandGui;
        this.islandManager = islandManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }
        if (!islandManager.hasIsland(player.getUniqueId())) {
            createIslandGui.open(player);
            return true;
        } else {
            menuGui.open(player);
            return true;
        }
    }
}

package stark.skyBlockTest2.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.item.GuiItems;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;
import java.util.UUID;

public class MembersGui {

    private final IslandManager islandManager;

    public MembersGui(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    public void open(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) return;

        Inventory inv = Bukkit.createInventory(new MenuHolder(), 27, "§8Członkowie wyspy");
        GuiBuilder gui = new GuiBuilder(inv);

        OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());
        gui.set(10, GuiItems.playerHead(owner, true));

        int slot = 12;
        for (UUID memberUUID : island.getMembers()) {
            if (slot > 26) break;
            OfflinePlayer member = Bukkit.getOfflinePlayer(memberUUID);
            gui.set(slot, GuiItems.playerHead(member, false));
            slot++;
        }

        gui.fill(GuiItems.grayBackground());
        gui.set(22, GuiItems.closeButton());

        player.openInventory(inv);
    }
}

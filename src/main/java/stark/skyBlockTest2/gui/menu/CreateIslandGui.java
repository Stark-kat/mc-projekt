package stark.skyBlockTest2.gui.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.item.GuiItems;

public class CreateIslandGui {

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(), 27, Component.text("Create Island"));

        new GuiBuilder(inventory)
                .set(12, GuiItems.book())
                .set(14, GuiItems.createIsland())
                .fill(GuiItems.greenBackground());
        player.openInventory(inventory);
    }
}

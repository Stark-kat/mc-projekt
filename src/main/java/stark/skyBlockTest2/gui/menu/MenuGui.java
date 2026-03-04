package stark.skyBlockTest2.gui.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.item.GuiItems;

public class MenuGui {

    public void open(Player player) {

            Inventory inventory = Bukkit.createInventory(new MenuHolder(), 54, Component.text("Menu"));

            new GuiBuilder(inventory)
                    .set(10, GuiItems.spawn())
                    .set(12, GuiItems.clock())
                    .set(14, GuiItems.home())
                    .set(28, GuiItems.upgradeSizeLvl())
                    .set(30, GuiItems.members())
                    .set(32, GuiItems.islandSettings())
                    .set(44, GuiItems.closeButton())
                    .fill(GuiItems.background());
            player.openInventory(inventory);
    }
}
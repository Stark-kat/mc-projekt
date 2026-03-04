package stark.skyBlockTest2.gui.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.item.GuiItems;

public class UpgradeIslandGui {

    private final Inventory inventory;

    public UpgradeIslandGui() {
        this.inventory = Bukkit.createInventory(new MenuHolder(), 27, Component.text("Upgrade island size"));

        new GuiBuilder(inventory)
                .set(9, GuiItems.lvl1())
                .set(11, GuiItems.lvl2())
                .set(13, GuiItems.lvl3())
                .set(15, GuiItems.lvl4())
                .set(17, GuiItems.lvl5())
                .set(26, GuiItems.closeButton())
                .fill(GuiItems.grayBackground());
    }
    public void open(Player player) {
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }
}

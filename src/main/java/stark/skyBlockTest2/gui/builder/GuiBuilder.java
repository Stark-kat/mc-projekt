package stark.skyBlockTest2.gui.builder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiBuilder {

    private final Inventory inventory;

    public GuiBuilder(Inventory inventory) {
        this.inventory = inventory;
    }

    public GuiBuilder set(int slot, ItemStack item) {
        inventory.setItem(slot, item);
        return this;
    }

    public GuiBuilder fill(ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item);
            }
        }
        return this;
    }
}
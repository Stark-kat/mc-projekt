package stark.skyBlockTest2.gui.builder;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import stark.skyBlockTest2.SkyBlockTest2;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder name(String name) {
        meta.setDisplayName(name);
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder lore(String... lines) {
        meta.setLore(Arrays.asList(lines));
        return this;
    }

    public ItemBuilder setString(String key, String value) {

        NamespacedKey namespacedKey = new NamespacedKey(SkyBlockTest2.getInstance(), key);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(namespacedKey, PersistentDataType.STRING, value);

        return this;
    }

    public static String getString(ItemStack item, String key) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        NamespacedKey namespacedKey = new NamespacedKey(SkyBlockTest2.getInstance(), key);
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
    }

    public ItemBuilder glow(boolean active) {
        if (active) {
            meta.addEnchant(Enchantment.LOYALTY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder hideAll() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    public ItemBuilder skull(OfflinePlayer player) {
        if (item.getType() == Material.PLAYER_HEAD) {
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) meta;
            skullMeta.setOwningPlayer(player);
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}

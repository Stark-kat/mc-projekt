package stark.skyBlockTest2.island;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SchematicManager {

    private final JavaPlugin plugin;
    private final File schematicFolder;

    public SchematicManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // Tworzymy folder /plugins/TwojPlugin/schematics/
        this.schematicFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicFolder.exists()) {
            schematicFolder.mkdirs();
        }
    }

    /*
     * Wkleja schemat w podanej lokalizacji.
     * @param loc Miejsce, w którym ma się pojawić punkt "Origin" schematu
     * @param schematicName Nazwa pliku (bez .schem)
     */
    public void pasteSchematic(Location loc, String schematicName) {
        File file = new File(schematicFolder, schematicName + ".schem");

        if (!file.exists()) {
            plugin.getLogger().warning("Nie znaleziono schematu: " + file.getName());
            return;
        }

        // 1. Wybieramy format pliku
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) return;

        // 2. Wczytujemy schemat do schowka (Clipboard)
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();

            // 3. Tworzymy sesję edycji (EditSession)
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(loc.getWorld()))) {

                // 4. Przygotowujemy operację wklejania
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()))
                        .ignoreAirBlocks(false) // Jeśli true, nie usunie bloków, które są w miejscu wklejania
                        .build();

                // 5. Wykonujemy wklejanie
                Operations.complete(operation);
                plugin.getLogger().info("Pomyślnie wklejono wyspę: " + schematicName);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd podczas wczytywania schematu!");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

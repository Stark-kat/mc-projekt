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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;

public class SchematicManager {

    private final JavaPlugin plugin;
    private final File schematicFolder;

    public SchematicManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schematicFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicFolder.exists()) {
            schematicFolder.mkdirs();
        }
    }

    /**
     * Wkleja schemat w podanej lokalizacji.
     * Odczyt pliku odbywa się asynchronicznie, wklejanie na głównym wątku.
     * Po zakończeniu wklejania wywoływany jest callback (może być null).
     *
     * @param loc           Lokalizacja środka wklejania (origin schematu)
     * @param schematicName Nazwa pliku bez rozszerzenia (np. "Default_Island")
     * @param onComplete    Runnable wykonany na głównym wątku po wklejeniu (może być null)
     */
    public void pasteSchematic(Location loc, String schematicName, Runnable onComplete) {
        World world = loc.getWorld();
        if (world == null) {
            plugin.getLogger().warning("[SchematicManager] Nie można wkleić schematu '"
                    + schematicName + "' — świat jest null.");
            return;
        }

        File file = new File(schematicFolder, schematicName + ".schem");
        if (!file.exists()) {
            plugin.getLogger().warning("[SchematicManager] Nie znaleziono schematu: " + file.getName()
                    + ". Upewnij się, że plik istnieje w folderze /schematics/.");
            return;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            plugin.getLogger().warning("[SchematicManager] Nieznany format pliku: " + file.getName());
            return;
        }

        // Snapshot lokalizacji — nie trzymamy referencji do Location, która może się zmienić
        final double x = loc.getX();
        final double y = loc.getY();
        final double z = loc.getZ();

        // Odczyt pliku i wklejanie asynchronicznie — nie blokujemy głównego wątku
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                Clipboard clipboard = reader.read();

                // WorldEdit paste musi wrócić na główny wątek
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try (EditSession editSession = WorldEdit.getInstance()
                            .newEditSession(BukkitAdapter.adapt(world))) {

                        Operation operation = new ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(BlockVector3.at(x, y, z))
                                .ignoreAirBlocks(false)
                                .build();

                        Operations.complete(operation);
                        plugin.getLogger().info("[SchematicManager] Wklejono schemat: " + schematicName);

                        if (onComplete != null) onComplete.run();

                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE,
                                "[SchematicManager] Błąd WorldEdit podczas wklejania schematu '"
                                        + schematicName + "'!", e);
                    }
                });

            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[SchematicManager] Błąd odczytu pliku schematu '" + schematicName + "'!", e);
            }
        });
    }
}
package stark.skyBlockTest2.island.generator.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.generator.GeneratorLevel;
import stark.skyBlockTest2.island.generator.GeneratorManager;
import stark.skyBlockTest2.island.generator.GeneratorType;

public class BlockFormGeneratorListener implements Listener {

    private final IslandManager islandManager;
    private final GeneratorManager generatorManager;

    public BlockFormGeneratorListener(IslandManager islandManager, GeneratorManager generatorManager) {
        this.islandManager  = islandManager;
        this.generatorManager = generatorManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        Material formed = event.getNewState().getType();

        // Interesują nas tylko cobblestone (overworld) i stone (cave)
        GeneratorType type = switch (formed) {
            case COBBLESTONE -> GeneratorType.COBBLESTONE;
            case STONE       -> GeneratorType.STONE;
            default          -> null;
        };
        if (type == null) return;

        // Sprawdzamy czy jesteśmy w odpowiednim świecie
        String worldName = event.getBlock().getWorld().getName();
        if (!type.worldName.equals(worldName)) return;

        Island island = islandManager.getIslandAt(event.getBlock().getLocation());
        if (island == null) return;

        GeneratorLevel level = generatorManager.getCurrentLevel(island.getOwner().toString(), type);
        if (level == null) return;

        Material result = level.roll();
        if (result == formed) return; // Brak zmiany — nie ruszamy stanu bloku

        event.getNewState().setType(result);
    }
}
package stark.skyBlockTest2.island.generator.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.generator.GeneratorDefinition;
import stark.skyBlockTest2.island.generator.GeneratorManager;
import stark.skyBlockTest2.island.generator.GeneratorMachineTracker;
import stark.skyBlockTest2.island.generator.GeneratorType;

public class MachineGeneratorListener implements Listener {

    private final IslandManager islandManager;
    private final GeneratorManager generatorManager;

    public MachineGeneratorListener(IslandManager islandManager, GeneratorManager generatorManager) {
        this.islandManager    = islandManager;
        this.generatorManager = generatorManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player    = event.getPlayer();
        Location loc     = event.getBlock().getLocation();
        Material placed  = event.getBlock().getType();
        String worldName = loc.getWorld().getName();

        GeneratorType type = GeneratorType.fromWorld(worldName);
        if (type == null || !type.isMachineType()) return;

        GeneratorDefinition def = generatorManager.getConfig().getDefinition(type);
        if (def == null || def.getMachineBlock() != placed) return;

        Island island = islandManager.getIslandAt(loc);
        if (island == null) return;

        // Sprawdź czy gracz jest memberem wyspy
        if (!island.isMember(player.getUniqueId())) {
            player.sendMessage("§cMożesz postawić maszynę tylko na własnej wyspie!");
            event.setCancelled(true);
            return;
        }

        String islandId = island.getOwner().toString();
        GeneratorMachineTracker tracker = generatorManager.getMachineTracker();
        tracker.register(loc, type, def, () -> generatorManager.getLevel(islandId, type));
        generatorManager.saveMachine(loc, type);
        player.sendMessage("§aGenerator " + type.name() + " uruchomiony!");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc     = event.getBlock().getLocation();
        String worldName = loc.getWorld().getName();

        GeneratorType type = GeneratorType.fromWorld(worldName);
        if (type == null || !type.isMachineType()) return;

        GeneratorDefinition def = generatorManager.getConfig().getDefinition(type);
        if (def == null || def.getMachineBlock() != event.getBlock().getType()) return;

        GeneratorMachineTracker tracker = generatorManager.getMachineTracker();
        if (!tracker.isRegistered(loc)) return;

        tracker.unregister(loc);
        generatorManager.deleteMachine(loc);
        event.getPlayer().sendMessage("§eGenerator " + type.name() + " zatrzymany.");
    }
}
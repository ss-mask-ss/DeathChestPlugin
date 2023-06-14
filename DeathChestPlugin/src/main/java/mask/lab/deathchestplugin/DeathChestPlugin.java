package mask.lab.deathchestplugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DeathChestPlugin extends JavaPlugin implements Listener {

    // Mapping of UUIDs to their death chests
    private final Map<UUID, Block> deathChests = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("DeathChestPlugin has been enabled.");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("DeathChestPlugin has been disabled.");
        removeDeathChests();
        deathChests.clear();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation();

        // Spawn a chest at the player's death location
        Block chest = deathLocation.getBlock();
        chest.setType(Material.CHEST);

        // Store the player's items in the chest
        Chest chestState = (Chest) chest.getState();
        chestState.getInventory().setContents(event.getDrops().toArray(new ItemStack[0]));

        // Clear the drops so they don't also drop on the ground
        event.getDrops().clear();

        // Register the chest with the player's UUID
        deathChests.put(player.getUniqueId(), chest);

        // Set a timer to remove the chest after 24 hours
        new BukkitRunnable() {
            @Override
            public void run() {
                removeDeathChest(player.getUniqueId());
            }
        }.runTaskLater(this, 24 * 60 * 60 * 20); // 24 hours in server ticks (20 ticks/second)
    }

    @EventHandler
    public void onChestOpen(PlayerInteractEvent event) {
        if (event.getAction().name().contains("RIGHT_CLICK")) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.CHEST) {
                // If the chest is a death chest and doesn't belong to the player, cancel the event
                if (deathChests.containsValue(block)) {
                    for (Map.Entry<UUID, Block> entry : deathChests.entrySet()) {
                        if (entry.getValue().equals(block) && !entry.getKey().equals(event.getPlayer().getUniqueId())) {
                            event.setCancelled(true);
                            event.getPlayer().sendMessage("This isn't your chest!");
                            break;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onExplosion(BlockExplodeEvent event) {
        if (deathChests.containsValue(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    private void removeDeathChest(UUID playerUUID) {
        Block chest = deathChests.remove(playerUUID);
        if (chest != null) {
            chest.setType(Material.AIR);
        }
    }

    private void removeDeathChests() {
        for (Block chest : deathChests.values()) {
            chest.setType(Material.AIR);
        }
    }
}

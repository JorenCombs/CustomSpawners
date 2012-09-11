package joren.customspawners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.inventory.ItemStack;

public class SpawnerBreakListener extends BlockListener{

	public static CustomSpawners plugin;
	
	public SpawnerBreakListener(CustomSpawners plugin) {
		PluginListener.plugin = plugin;
	}
	
	public void onBlockBreak(BlockBreakEvent event)
	{	
		if (event.isCancelled())
			return;
		Player player = event.getPlayer();
		Block block = event.getBlock();
		if ((player != null)&&(block != null))
		{
			if (block.getType() == Material.MOB_SPAWNER)
			{
				if (CustomSpawners.allowedTo(player, "customspawners.pickupspawner"))
				{
					Location location = player.getLocation();
					player.getWorld().dropItemNaturally(location, new ItemStack(Material.MOB_SPAWNER, 1));
				}
			}
		}
	}
}

package joren.customspawners;

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

import com.nijikokun.bukkit.Permissions.Permissions;

public class PluginListener extends ServerListener{

	public static CustomSpawners plugin;
	
	public PluginListener(CustomSpawners plugin) {
		PluginListener.plugin = plugin;
	}
	
	public void onPluginDisable(PluginDisableEvent event)
	{
		if (event.getPlugin().getDescription().getName().equals("Permissions"))
		{
			if (CustomSpawners.permissions != null)
			{
				CustomSpawners.permissions = null;
				CustomSpawners.warning("Unlinked with Permissions - falling back to ops-only mode.");
			}
		}
	}

	public void onPluginEnable(PluginEnableEvent event)
	{
		if (event.getPlugin().getDescription().getName().equals("Permissions"))
		{
			if (CustomSpawners.permissions == null)
			{
				CustomSpawners.permissions = ((Permissions) event.getPlugin()).getHandler();
				CustomSpawners.info("Linked with Permissions");
			}
		}
	}

}

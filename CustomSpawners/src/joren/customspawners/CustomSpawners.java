package joren.customspawners;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * SpawnMobLite - Main
 * @version 0.1
 * @author jorencombs
 * 
 * Made with much rewriting of code; original SpawnMob Bukkit adaptation by jordanneil23.
 */
@SuppressWarnings("deprecation")
public class CustomSpawners extends JavaPlugin {
//	private final PListener playerListener = new PListener(this);
	public static java.util.logging.Logger log = java.util.logging.Logger.getLogger("Minecraft");
	public static PermissionHandler permissions;
	private final PluginListener pluginListener = new PluginListener(this);
	private final SpawnerBreakListener spawnerBreakListener = new SpawnerBreakListener(this);

	/** Name of the plugin, used in output messages */
	protected static String name = "CustomSpawners";
	/** Path where the plugin's saved information is located */
	protected static String path = "plugins" + File.separator + name;
	/** Location of the config YML file */
	protected static String config = path + File.separator + name + ".yml";
	/** Header used for console and player output messages */
	protected static String header = "[" + name + "] ";
	protected static List<String> neverSpawn = new ArrayList<String>();
	protected static Configuration cfg = null;
	
	public void onEnable()
	{
		PluginDescriptionFile pdfFile = this.getDescription();
		name = pdfFile.getName();
		header = "[" + name + "] ";
		path = "plugins" + File.separator + name;
		config = path + File.separator + name + ".yml";
		reload();
		info("Version " + pdfFile.getVersion() + " enabled.");
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLUGIN_ENABLE, this.pluginListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLUGIN_DISABLE, this.pluginListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, this.spawnerBreakListener, Priority.Monitor, this);
		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");
		if (permissions == null) {
			if (test != null)
			{
				permissions = ((Permissions)test).getHandler();
				info("Linked with Permissions");
			} 
			else
			{
				info("Could not link with Permissions - falling back to ops-only.");
			}
		}

	}
	
	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		log.info(header + "Version " + pdfFile.getVersion() + " disabled.");
	}
	
	/**
	 * Reloads the plugin by re-reading the configuration file and setting associated variables
	 * 
	 * The configuration will be replaced with whatever information is in the file.  Any variables that need to be read from the configuration will be initialized.
	 * 
	 * @return boolean: True if reload was successful.  Currently all reloads are considered successful
	 * since there are fallbacks for cases where the configuration isn't there.
	 */
	public boolean reload()
	{
		info("(re)loading...");
		File file = new File(config);
		cfg = new Configuration(file);
		if(!file.exists())
		{
			warning("Could not find a configuration file, saving a new one...");
			if (!saveDefault())
			{
				warning("Running on default values, but could not save a new configuration file.");
			}
		}
		else
		{
			cfg.load();
			neverSpawn = cfg.getStringList("never.spawn", neverSpawn);
		}
		info("done.");
		return true;
	}

	/**
	 * Saves a new default configuration file, overwriting old configuration and file in the process
	 * Any existing configuration will be replaced with the default configuration and saved to disk.  Any variables that need to be read from the configuration will be initialized
	 * @return boolean: True if successful, false otherwise
	 */
	public boolean saveDefault()
	{
		info("Resetting configuration file with list of CreatureTypes currently supported by Bukkit...");
		cfg = new Configuration(new File(config));

		CreatureType[] types = CreatureType.class.getEnumConstants();
		for (int i=0; i<types.length; i++)
			cfg.setProperty("alias." + types[i].getName().toLowerCase(), types[i].getName());
		
		if (save())
		{
			reload();
			return true;
		}
		else
			return false;
	}
	
	/**
	 * Saves the configuration file, overwriting old file in the process
	 * 
	 * @return boolean: True if successful, false otherwise.
	 */
	public boolean save()
	{
		info("Saving configuration file...");
		File dir = new File(path);
		cfg.setProperty("never.spawn", neverSpawn);
		if(!dir.exists())
		{
			if (!dir.mkdir())
			{
				severe("Could not create directory " + path + "; if there is a file with this name, please rename it to something else.  Please make sure the server has rights to make this directory.");
				return false;
			}
			info("Created directory " + path + "; this is where your configuration file will be kept.");
		}
		cfg.save();
		File file = new File(config);
		if (!file.exists())
		{
			severe("Configuration could not be saved! Please make sure the server has rights to output to " + config);
			return false;
		}
		info("Saved configuration file: " + config);
		return true;
	}
	
	/**
	 * Sets a flag intended to prevent this entity from ever being spawned by this plugin
	 * 
	 * This is intended for situations where the entity threw an exception indicating that the
	 * game really, really, really was not happy about being told to spawn that entity.  Flagging
	 * this entity is supposed to stop any player (even the admin) from spawning this entity
	 * regardless of permissions, aliases, etc.
	 * 
	 * @param ent - The entity class.  No instance of this class will be spawned using this plugin
	 */
	
	public void flag(Class<Entity> ent)
	{
		if (neverSpawn.contains(ent.getSimpleName()))
			return;
		neverSpawn.add(ent.getSimpleName());
	}

	
	/**
	 * Checks to see if sender has permission OR is an op.  If not using permissions, only op is tested.
	 * @param sender: The person whose permission is being checked
 	 * @param permission The permission being checked (e.g. "exampleplugin.examplepermnode")
	 * @returns boolean: True if player has the permission node OR if player is an op
	 */
	protected static boolean allowedTo(CommandSender sender, String permission)
	{
		if (sender.isOp())
			return true;
		if (permissions!=null && sender instanceof Player)
			return permissions.has((Player)sender, permission);
		return false;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		if (sender instanceof ConsoleCommandSender)
			return false;
		Player player = (Player) sender;
		if (command.getName().equalsIgnoreCase("customspawner") || command.getName().equalsIgnoreCase("spawner") || command.getName().equalsIgnoreCase("cs"))
		{
			int[] ignore = {8, 9}; // Ignore these block types
			int delay=-1;
			CreatureType type = null;
			Block block = (new TargetBlock(player, 300, 0.2, ignore)).getTargetBlock();
			if (block.getTypeId()!=52)
				return false;
			CreatureSpawner cs = (CreatureSpawner)block.getState();//Should never fail to cast
			if (args.length > 0)
			{
				for (int i=0; i<args.length; i++)
				{
					if (type == null)
						type = lookup(args[i]);
					try
					{
						delay = Integer.parseInt(args[i]) * 20;
					}
					catch (NumberFormatException e)
					{
						//nothing
					}
				}
				if (type != null)
				{
					if (allowedTo(player, "customspawners." + type.getName().toLowerCase()))
						cs.setCreatureType(type);
				}
				if (delay != -1)
				{
					if (allowedTo(player, "customspawners.setdelay"))
						cs.setDelay(delay);
				}
			}
			player.sendMessage("Spawner is set to spawn " + cs.getCreatureTypeId() + "s in about... " + cs.getDelay()/20 + " seconds from now.");
			return true;
		}
//		player.sendMessage("Unknown console command. Type \"help\" for help"); // No reason to tell them what they CAN'T do, right?
		return false;

	}
	
	/**
	 * Utility function; returns the CreatureType associated with the supplied alias from the configuration file.
	 * 
	 * @param alias: An alias/name associated with one or more entity types
	 * @param sender: The person who asked for the alias
	 * @return CreatureType: The CreatureType referred to by the supplied alias.  If alias did not refer to anything, returns null.
	 */
	public CreatureType lookup(String alias)
	{
		CreatureType type = null;
		if (alias == null)
			return null;
		String name = cfg.getString("alias." + alias.toLowerCase(), null);
		if (name == null) // Assume that this is an unknown type and that player is trying a case-sensitive CreatureType name from the enum.
		{
			type = CreatureType.fromName(alias);
			if (type != null) //It worked, so add it in as a new CreatureType
			{
				cfg.setProperty("alias." + alias.toLowerCase(), alias);
				info("CreatureType " + alias + " has not been invoked before; adding alias to configuration");
			}
		}
		else // Lookup from config file succeeded, now try getting a CreatureType from it
		{
			type = CreatureType.fromName(name);
		}
		return type;
	}

	/**
	 * Logs an informative message to the console, prefaced with this plugin's header
	 * @param message: String
	 */
	protected static void info(String message)
	{
		log.info(header + message);
	}

	/**
	 * Logs a severe error message to the console, prefaced with this plugin's header
	 * Used to log severe problems that have prevented normal execution of the plugin
	 * @param message: String
	 */
	protected static void severe(String message)
	{
		log.severe(header + message);
	}

	/**
	 * Logs a warning message to the console, prefaced with this plugin's header
	 * Used to log problems that could interfere with the plugin's ability to meet admin expectations
	 * @param message: String
	 */
	protected static void warning(String message)
	{
		log.warning(message);
	}

	/**
	 * Logs a message to the console, prefaced with this plugin's header
	 * @param level: Logging level under which to send the message
	 * @param message: String
	 */
	protected static void log(java.util.logging.Level level, String message)
	{
		log.log(level, header + message);
	}
}


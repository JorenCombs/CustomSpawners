package joren.customspawners;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * SpawnMobLite - Main
 * @version 0.1
 * @author jorencombs
 * 
 * Made with much rewriting of code; original SpawnMob Bukkit adaptation by jordanneil23.
 */
public class CustomSpawners extends JavaPlugin
    implements org.bukkit.event.Listener, org.bukkit.command.CommandExecutor {
    
    public static java.util.logging.Logger log = java.util.logging.Logger.getLogger("Minecraft");
    public static PermissionHandler permissions;

    /** Name of the plugin, used in output messages */
    protected static String pluginName = "CustomSpawners";
    /** Location of the config YML file */
    protected static File configFile;
    /** Header used for console and player output messages */
    protected static String header = "[" + pluginName + "] ";
    protected static List<String> neverSpawn = new ArrayList<String>();
    static YamlConfiguration cfg = null;
    
    public void onEnable()
    {
        PluginDescriptionFile pdfFile = this.getDescription();
        pluginName = pdfFile.getName();
        header = "[" + pluginName + "] ";
        configFile = new File(getDataFolder(), pluginName + ".yml");
        reload();
        info("Version " + pdfFile.getVersion() + " enabled.");
        getServer().getPluginManager().registerEvents(this, this);

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
        cfg = new YamlConfiguration();
        if(!configFile.exists()) {
            warning("Could not find a configuration file, saving a new one...");
            initDefault(cfg);
            save(cfg);
        }
        else {
            // This block of code pretty well illustrates why I fucking hate Java.
            try {
                cfg.load(configFile);
            } catch (java.io.FileNotFoundException e) {
                e.printStackTrace();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } catch (org.bukkit.configuration.InvalidConfigurationException e) {
                e.printStackTrace();
            }

        }
        neverSpawn = cfg.getStringList("never.spawn");

        info("done.");
        return true;
    }

    /**
     * Saves a new default configuration file, overwriting old configuration and file in the process
     * Any existing configuration will be replaced with the default configuration and saved to disk.  Any variables that need to be read from the configuration will be initialized
     * @return boolean: True if successful, false otherwise
     */
    public void initDefault(ConfigurationSection cfg)
    {
        EntityType[] types = EntityType.class.getEnumConstants();
        for (int i=0; i<types.length; i++) {
            cfg.set("alias." + types[i].getName().toLowerCase(), types[i].getName());
        }
    }
    
    /**
     * Saves the configuration file, overwriting old file in the process
     * 
     * @return boolean: True if successful, false otherwise.
     */
    public boolean save(YamlConfiguration cfg)
    {
        cfg.set("never.spawn", neverSpawn);
        try {
            cfg.save(configFile);
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        //info("Saved configuration file: " + configFile);
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
            HashSet<Byte> ignoreBlocks = new HashSet<Byte>(); // Ignore these block types
            ignoreBlocks.add((byte)8);
            ignoreBlocks.add((byte)9);
        
            int delay=-1;
            EntityType type = null;
            Block block = player.getTargetBlock(ignoreBlocks, 300);
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
                        cs.setSpawnedType(type);
                }
                if (delay != -1)
                {
                    if (allowedTo(player, "customspawners.setdelay"))
                        cs.setDelay(delay);
                }
            }
            player.sendMessage("Spawner is set to spawn " + cs.getCreatureTypeName() + "s in about... " + cs.getDelay()/20 + " seconds from now.");
            return true;
        }
//        player.sendMessage("Unknown console command. Type \"help\" for help"); // No reason to tell them what they CAN'T do, right?
        return false;

    }
    
    /**
     * Utility function; returns the EntityType associated with the supplied alias from the configuration file.
     * 
     * @param alias: An alias/name associated with one or more entity types
     * @param sender: The person who asked for the alias
     * @return EntityType: The EntityType referred to by the supplied alias.  If alias did not refer to anything, returns null.
     */
    public EntityType lookup(String alias)
    {
        EntityType type = null;
        if (alias == null)
            return null;
        String name = cfg.getString("alias." + alias.toLowerCase(), null);
        if (name == null) // Assume that this is an unknown type and that player is trying a case-sensitive EntityType name from the enum.
        {
            type = EntityType.fromName(alias);
            if (type != null) //It worked, so add it in as a new EntityType
            {
                cfg.set("alias." + alias.toLowerCase(), alias);
                info("EntityType " + alias + " has not been invoked before; adding alias to configuration");
            }
        }
        else // Lookup from config file succeeded, now try getting a EntityType from it
        {
            type = EntityType.fromName(name);
        }
        return type;
    }

    public static YamlConfiguration loadInfoFile(File file) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        } catch (org.bukkit.configuration.InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return config;
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
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
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
                    block.setTypeId(0);
                }
            }
        }
    }
    
    @EventHandler
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

    @EventHandler
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


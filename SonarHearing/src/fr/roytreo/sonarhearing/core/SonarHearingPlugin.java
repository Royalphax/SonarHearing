package fr.roytreo.sonarhearing.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.inventivetalent.glow.GlowAPI.Color;

import fr.roytreo.sonarhearing.core.command.SonarHearingCommand;
import fr.roytreo.sonarhearing.core.handler.Entities;
import fr.roytreo.sonarhearing.core.manager.ExceptionManager;
import fr.roytreo.sonarhearing.core.manager.URLManager;
import fr.roytreo.sonarhearing.core.stat.DataRegister;
import fr.roytreo.sonarhearing.core.task.SneakyTask;
import fr.roytreo.sonarhearing.core.util.Utils;
import fr.roytreo.sonarhearing.core.version.INMS;

/**
 * @author Roytreo28
 */
public class SonarHearingPlugin extends JavaPlugin implements Listener {
	
	public static String user_id = "%%__USER__%%";
	public static String download_id = "%%__NONCE__%%";	
	
	public Server SERVER;
	public Double RADIUS;
	public Boolean UPDATE;
	public Boolean RED_ALERT;
	public Boolean HEARTBEAT;
	public Boolean TAMED_MOB;
	public Boolean POST_V13;
	public Integer DELAY;
	public Boolean FREEZE;
	public Color TAMED_MOB_COLOR;
	public INMS INMS;
	public Boolean LOCAL_HOST;
	public HashMap<Player, BukkitRunnable> playerTask;
	public File entitiesFile;
	public FileConfiguration entitiesConfig;
	public File blocklistFile;
	public FileConfiguration blocklistConfig;
	public ArrayList<String> disablePlayersAbility;
	public ArrayList<CommandSender> debuggers;
	public static ArrayList<Material> blocklist;

	static {
		blocklist = new ArrayList<>();
	}

	@Override
	public void onEnable() {
		this.SERVER = getServer();
		this.UPDATE = false;
		this.LOCAL_HOST = false;
		this.POST_V13 = false;
		this.playerTask = new HashMap<>();
		this.disablePlayersAbility = new ArrayList<>();
		this.debuggers = new ArrayList<>();
		
		if (!setupNMS())
		{
			getLogger().warning("Your server version isn't supported by the plugin.");
			getPluginLoader().disablePlugin(this);
			return;
		}
		
		this.saveDefaultConfig();
		this.DELAY = getConfig().getInt("delay");
		this.FREEZE = getConfig().getBoolean("freeze");
		
		this.TAMED_MOB = getConfig().getBoolean("tamed-mob.force-color");
		try {
			this.TAMED_MOB_COLOR = Color.valueOf(getConfig().getString("tamed-mob.color"));
		} catch (IllegalArgumentException ex) {
			getLogger().warning("Tamed mob color specified in config.yml wasn't recognized. Default one selected: BLUE.");
			this.TAMED_MOB_COLOR = Color.BLUE;
		}
		
		entitiesFile = new File(getDataFolder(), "entities.yml");
		if (!entitiesFile.exists())
			saveResource("entities.yml", true);
		entitiesConfig = YamlConfiguration.loadConfiguration(entitiesFile);
		entitiesConfig.options().copyDefaults(true);
		entitiesConfig.options().copyHeader(true);
		blocklistFile = new File(getDataFolder(), "blocklist.yml");
		if (!blocklistFile.exists())
			saveResource("blocklist.yml", true);
		blocklistConfig = YamlConfiguration.loadConfiguration(blocklistFile);

		for (String material : blocklistConfig.getStringList("blockList")) {
			if (Material.getMaterial(material) != null) {
				blocklist.add(Material.getMaterial(material));
			} else {
				this.getLogger().warning("<!> Material \"" + material + "\" was not recognized in blocklist.yml");
			}
		}
		File disablePlayers = new File(getDataFolder(), "disablePlayers.yml");
		if (!disablePlayers.exists())
			saveResource("disablePlayers.yml", true);
		FileConfiguration disablePlayersConfig = YamlConfiguration.loadConfiguration(disablePlayers);
		disablePlayersConfig.options().copyDefaults(true);
		disablePlayersConfig.options().copyHeader(true);
		for (String player : disablePlayersConfig.getStringList("disablePlayersList"))
			disablePlayersAbility.add(player);
		
		RED_ALERT = getConfig().getBoolean("effects.red-alert");
		HEARTBEAT = getConfig().getBoolean("effects.heartbeat");

		for (EntityType type : EntityType.values())
		{
			if (type != EntityType.COMPLEX_PART && type != EntityType.UNKNOWN) {
				Color color = Color.WHITE;
				Double radius = 10.0D;
				Boolean enable = true;
				if (entitiesConfig.contains(type.toString()))
				{
					try {
						color = Color.valueOf(entitiesConfig.getString(type.toString() + ".color"));
					} catch (IllegalArgumentException ex) { color = null; }
					radius = entitiesConfig.getDouble(type.toString() + ".radius");
					enable = entitiesConfig.getBoolean(type.toString() + ".enable");
				} else {
					entitiesConfig.set(type.toString() + ".enable", true);
					entitiesConfig.set(type.toString() + ".radius", 10.0D);
					entitiesConfig.set(type.toString() + ".color", Color.WHITE.toString());
					try {
						entitiesConfig.save(entitiesFile);
					} catch (IOException e) {
						new ExceptionManager(e).register(this, true);
					}
				}
				if (color == null) 
					getLogger().info("<!> The specified color for " + type.toString() + " doesn't exist anymore.");
				new Entities(type, (color == null ? Color.WHITE : color), radius, enable);
			}
		}
		
		this.RADIUS = fr.roytreo.sonarhearing.core.handler.Entities.getMaxRadius() + 5.0D;
		this.getServer().getPluginManager().registerEvents(this, this);
		this.getCommand("sonarhearing").setExecutor(new SonarHearingCommand(this));
		
		new BukkitRunnable()
		{
			public void run()
			{
				if (!URLManager.checkVersion(getDescription().getVersion(), false, URLManager.Link.GITHUB_PATH)) {
					getLogger().warning("A new version more efficient of the plugin is available. Do '/sh update' to automatically update the plugin.");
					UPDATE = true;
				} else {
					getLogger().info("Plugin is up-to-date.");
				}
			}
		}.runTaskAsynchronously(this);
		new DataRegister(this, false, false);
	}
	
	@Override
	public void onDisable() {
		File disablePlayers = new File(getDataFolder(), "disablePlayers.yml");
		if (!disablePlayers.exists())
			saveResource("disablePlayers.yml", true);
		FileConfiguration disablePlayersConfig = YamlConfiguration.loadConfiguration(disablePlayers);
		disablePlayersConfig.options().copyDefaults(true);
		disablePlayersConfig.options().copyHeader(true);
		disablePlayersConfig.set("disablePlayersList", disablePlayersAbility);
		try {
			disablePlayersConfig.save(disablePlayers);
		} catch (IOException e) {
			new ExceptionManager(e).register(this, true);
		}
	}

	@EventHandler
	public void onSneak(final PlayerToggleSneakEvent event) {
		final SonarHearingPlugin instance = this;
		final Player player = event.getPlayer();
		if (event.isSneaking() && Utils.isAgainstBlocks(player) && player.hasPermission("sonar.hearing.use") && !disablePlayersAbility.contains(player.getName())) {
			new BukkitRunnable()
			{
				int i = 20*DELAY;
				public void run()
				{
					i--;
					if (!event.isSneaking() || !Utils.isAgainstBlocks(player) || !player.hasPermission("sonar.hearing.use")) {
						this.cancel();
					}
					if (i <= 0)
					{
						this.cancel();
						if (playerTask.containsKey(player))
							playerTask.get(player).cancel();
						SneakyTask task = new SneakyTask(instance, player);
						playerTask.put(player, task);
					}
				}
			}.runTaskTimer(this, 0, 0);
		}
	}
	
	public Boolean setupNMS() {
		String version;
		try {
			version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
		} catch (ArrayIndexOutOfBoundsException unsuportedVersion) {
			return false;
		}
		switch (version) {
		case "v1_9_R1":
			INMS = new fr.roytreo.sonarhearing.v1_9_R1.NMS();
			break;
		case "v1_9_R2":
			INMS = new fr.roytreo.sonarhearing.v1_9_R2.NMS();
			break;
		case "v1_10_R1":
			INMS = new fr.roytreo.sonarhearing.v1_10_R1.NMS();
			break;
		case "v1_11_R1":
			INMS = new fr.roytreo.sonarhearing.v1_11_R1.NMS();
			break;
		case "v1_12_R1":
			INMS = new fr.roytreo.sonarhearing.v1_12_R1.NMS();
			break;
		case "v1_13_R1":
			POST_V13 = true;
			INMS = new fr.roytreo.sonarhearing.v1_13_R1.NMS();
			break;
		case "v1_13_R2":
			POST_V13 = true;
			INMS = new fr.roytreo.sonarhearing.v1_13_R2.NMS();
			break;
		case "v1_14_R1":
			POST_V13 = true;
			INMS = new fr.roytreo.sonarhearing.v1_14_R1.NMS();
			getLogger().warning("Due to not 1.14 compatibility of GlowAPI, SonarHearing can't be used under that version.");
			getServer().getPluginManager().disablePlugin(this);
			break;
		}
		
		return true;
	}
	
	public void deletePluginsJar() {
		if (!getFile().delete())
			getFile().deleteOnExit();
	}
}

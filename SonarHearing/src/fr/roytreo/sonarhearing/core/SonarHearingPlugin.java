package fr.roytreo.sonarhearing.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.command.Command;
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
import org.inventivetalent.glow.GlowAPI;
import org.inventivetalent.glow.GlowAPI.Color;

import fr.roytreo.sonarhearing.core.handler.Entities;
import fr.roytreo.sonarhearing.core.handler.URLManager;
import fr.roytreo.sonarhearing.core.stat.DataRegister;
import fr.roytreo.sonarhearing.core.task.SneakyTask;
import fr.roytreo.sonarhearing.core.util.Utils;
import fr.roytreo.sonarhearing.core.version.INMS;

/**
 * @author Roytreo28
 */
public class SonarHearingPlugin extends JavaPlugin implements Listener {
	public Server SERVER;
	public Double RADIUS;
	public Boolean UPDATE;
	public SonarHearingPlugin INSTANCE;
	public Boolean RED_ALERT;
	public Boolean HEARTBEAT;
	public Boolean TAMED_MOB;
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
		this.INSTANCE = this;
		this.UPDATE = false;
		this.LOCAL_HOST = false;
		this.playerTask = new HashMap<>();
		this.disablePlayersAbility = new ArrayList<>();
		this.debuggers = new ArrayList<>();
		
		if (!checkVersion())
		{
			getLogger().warning("Unsupported version, sorry :(");
			getPluginLoader().disablePlugin(this);
			return;
		}
		
		this.saveDefaultConfig();
		this.DELAY = getConfig().getInt("delay");
		this.FREEZE = getConfig().getBoolean("freeze");
		
		this.getLogger().info("Tamed mob section in config isn't working as expected at the moment, be patient. :)");
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
						getLogger().warning("CAN'T SAVE ENTITIES.YML, WHAT THE F*CK ? | ERROR: " + e.getMessage());
					}
				}
				if (color == null) getLogger().info("<!> The specified color for " + type.toString() + " doesn't exist anymore.");
				new Entities(type, (color == null ? Color.WHITE : color), radius, enable);
			}
		}
		
		this.RADIUS = fr.roytreo.sonarhearing.core.handler.Entities.getMaxRadius() + 5.0D;
		this.getServer().getPluginManager().registerEvents(this, this);
		
		new BukkitRunnable()
		{
			public void run()
			{
				if (!URLManager.isUpToDate(getDescription().getVersion(), false, URLManager.Values.GITHUB_PATH))
				{
					getLogger().info("A new version more efficient of the plugin is available. It will be automatically updated when the server will switch off.");
					UPDATE = true;
				} else {
					getLogger().info("Plugin is up-to-date");
				}
				new DataRegister(INSTANCE, LOCAL_HOST);
			}
		}.runTaskAsynchronously(this);
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
			e.printStackTrace();
		}
		
		if (this.UPDATE && URLManager.update(this, URLManager.getLatestVersion(), false, URLManager.Values.GITHUB_PATH))
		{
			getLogger().info("Stay informed about what the update bring new at https://www.spigotmc.org/resources/sonar-hearing-1-9-1-10-1-11.22640/updates");
			getFile().delete();
			getFile().deleteOnExit();
		}
	}

	@Override
	public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("sonarhearing")) {
			if (args.length > 0)
			{
				if (this.debuggers.contains(sender))
				{
					sender.sendMessage(ChatColor.RED + "No command allowed during debugging :(");
					return false;
				}
				if (args.length == 1)
				{
					if (sender instanceof Player) {
						Player player = (Player) sender;
						if (args[0].equalsIgnoreCase("on")) {
							if (!player.hasPermission("sonar.hearing.toggle")) {
								player.sendMessage(ChatColor.RED + "You are not allowed to do that.");
								return false;
							}
							if (disablePlayersAbility.contains(player.getName())) disablePlayersAbility.remove(player.getName());
							player.sendMessage("§7Your sonar hearing ability is now §aenable§7.");
						} else if (args[0].equalsIgnoreCase("off")) {
							if (!player.hasPermission("sonar.hearing.toggle")) {
								player.sendMessage(ChatColor.RED + "You are not allowed to do that.");
								return false;
							}
							if (!disablePlayersAbility.contains(player.getName())) disablePlayersAbility.add(player.getName());
							player.sendMessage("§7Your sonar hearing ability is now §cdisable§7.");
						}
						return true;
					}
				} else if (args.length == 2) 
				{
					String playerArg = args[1];
					if (Bukkit.getPlayer(playerArg) != null)
					{
						final Player bukkitPlayer = Bukkit.getPlayer(playerArg);
						if (args[0].equalsIgnoreCase("on")) {
							if (!sender.hasPermission("sonar.hearing.toggle.other")) {
								sender.sendMessage(ChatColor.RED + "You are not allowed to do that.");
								return false;
							}
							if (disablePlayersAbility.contains(bukkitPlayer.getName())) disablePlayersAbility.remove(bukkitPlayer.getName());
							sender.sendMessage("§7" + bukkitPlayer.getName() + "'s sonar hearing ability is now §aenable§7.");
						} else if (args[0].equalsIgnoreCase("off")) {
							if (!sender.hasPermission("sonar.hearing.toggle.other")) {
								sender.sendMessage(ChatColor.RED + "You are not allowed to do that.");
								return false;
							}
							if (!disablePlayersAbility.contains(bukkitPlayer.getName())) disablePlayersAbility.add(bukkitPlayer.getName());
							sender.sendMessage("§7" + bukkitPlayer.getName() + "'s sonar hearing ability is now §cdisable§7.");
						} else if (args[0].equalsIgnoreCase("debug")) {
							if (!sender.isOp() || this.debuggers.contains(sender) || !(sender instanceof Player)) {
								sender.sendMessage(ChatColor.RED + "You are not allowed to do that." + (!sender.isOp() ? " (Not an Operator)" : "") + (this.debuggers.contains(sender) ? " (You can't use the debug service twice at the same time)" : "") + (!(sender instanceof Player) ? " (You're not a player)" : ""));
								return false;
							}
							this.debuggers.add(sender);
							if (!disablePlayersAbility.contains(sender.getName())) disablePlayersAbility.add(sender.getName());
							sender.sendMessage("§a--- Starting a new debugging session ---");
							sender.sendMessage("§7We will try to debug §a" + bukkitPlayer.getName() + "§7's glowing effect ...");
							sender.sendMessage("§7 To do this, we will see if the player is considered as glowing by the server and by you ...");
							new BukkitRunnable()
							{
								int count = 0;
								int serverSide = 0;
								int apiSide = 0;
								public void run()
								{
									if (Utils.isGlowing(bukkitPlayer))
										serverSide++;
									if (GlowAPI.isGlowing(bukkitPlayer, (Player) sender))
										apiSide++;
									count++;
									if (count >= 100)
									{
										cancel();
										sender.sendMessage("§eWe did " + count + " tests:");
										sender.sendMessage("§7- §a" + serverSide + " §7time(s) the server was saying that the player was glowing.");
										sender.sendMessage("§7- §a" + apiSide + " §7time(s) it seems that you were seeing the player as glowing.");
										sender.sendMessage("§eConclusions: ");
										if (Utils.inRange(serverSide, 95, 100))
											sender.sendMessage("§7- We are thinking that the player has recently used a glowing potion.");
										if (Utils.inRange(apiSide, 95, 100))
											sender.sendMessage("§7- It seems that the player is constantly glowing for you.");
										if (Utils.inRange(serverSide, 30, 70))
											sender.sendMessage("§7- Is there a command block which adds potion effects ?");
										if (Utils.inRange(apiSide, 30, 70)) {
											sender.sendMessage("§7- It seems that the player's glowing effect is flickering for you.");
											sender.sendMessage("§7 Let's make a new test! But now, we will switch off the glowing effect of the player and see if you're always seeing it.");
											GlowAPI.setGlowing(bukkitPlayer, false, (Player) sender);
											new BukkitRunnable()
											{
												int count2 = 0;
												int apiSide2 = 0;
												public void run()
												{
													if (GlowAPI.isGlowing(bukkitPlayer, (Player) sender))
														apiSide2++;
													count2++;
													if (count2 >= 100)
													{
														cancel();
														sender.sendMessage("§eWe did " + count2 + " tests:");
														sender.sendMessage("§7- §a" + apiSide2 + " §7time(s) it seems that you were seeing the player as glowing.");
														sender.sendMessage("§eConclusions: ");
														if (Utils.inRange(apiSide2, 30, 100))
															sender.sendMessage("§7- Oh my god! There is a serious bug who prevent the plugin to actually stop the glowing effect of the player.");
														if (Utils.inRange(apiSide2, 0, 5))
															sender.sendMessage("§7- All seems to be good, we were just needing to stop the glowing effect of the player.");
														sender.sendMessage("§7If you were having an issue with a player who was flickering, please take a screen of all the chat during this debugging session and send it to the developer on spigot, thanks.");
													}
												}
											}.runTaskTimer(INSTANCE, 20*6, 1);
										}
										if (Utils.inRange(serverSide, 0, 5))
											sender.sendMessage("§7- The player has probably no glowing potion effect.");
										if (Utils.inRange(apiSide, 0, 5) && !Utils.inRange(serverSide, 95, 100))
											sender.sendMessage("§7- The player seems to be not glowing for you.");
										sender.sendMessage("§c--- End of the debugging session ---");
										if (disablePlayersAbility.contains(sender.getName())) disablePlayersAbility.remove(sender.getName());
										debuggers.remove(sender);
									}
								}
							}.runTaskTimer(this, 20*3, 1);
						}
						return true;
					} else {
						sender.sendMessage(ChatColor.RED + "This player isn't online.");
					}
				}
				sender.sendMessage(ChatColor.RED + "Invalid command, try /sh.");
				return false;
			}
			sender.sendMessage("§a§l§m=============================================");
			sender.sendMessage("");
			sender.sendMessage(ChatColor.GRAY
					+ "Sonar hearing is a light weight plugin which allows you to detect entities around you when sneaking. This plugin is perfect for DayZ/LastOfUs/ZombieMania servers."
					+ " If you need it, you can disable/enable your ability by doing §e/sh [on/off]§7.");
			if (sender.isOp())
				sender.sendMessage(ChatColor.GRAY
						+ "It seems that your are an Administrator of the server, so you can edit the file configuration of the plugin to suit your needs about mobs detection radius, blocks list, etcetera. Also remember that you can do §e/sh [on/off] <player>§7 to toggle the ability of your players.");
			sender.sendMessage("");
			sender.sendMessage(ChatColor.GREEN.toString() + ChatColor.UNDERLINE + "Thanks to :");
			sender.sendMessage("");
			sender.sendMessage(ChatColor.WHITE + "    - SQRTdude for the concept");
			sender.sendMessage(ChatColor.WHITE + "    - Asynchronous the developer");
			sender.sendMessage("");
			sender.sendMessage("§a§l§m=============================================");
			if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
			return true;
		}
		return false;
	}

	@EventHandler
	public void onSneak(final PlayerToggleSneakEvent event) {
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
						SneakyTask task = new SneakyTask(INSTANCE, player);
						playerTask.put(player, task);
					}
				}
			}.runTaskTimer(INSTANCE, 0, 0);
		}
	}
	
	public Boolean checkVersion() {
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
		}
		return true;
	}
}

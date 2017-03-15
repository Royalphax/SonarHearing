package fr.roytreo.sonarhearing.core.task;

import java.util.HashMap;

import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.inventivetalent.glow.GlowAPI;

import fr.roytreo.sonarhearing.core.SonarHearingPlugin;
import fr.roytreo.sonarhearing.core.handler.Entities;
import fr.roytreo.sonarhearing.core.util.Utils;

/**
 * @author Roytreo28
 */
public class SneakyTask extends BukkitRunnable {

	private static HashMap<OfflinePlayer, Team> playerTeam;

	static {
		playerTeam = new HashMap<>();
	}

	private Player PLAYER;
	private Integer HEARTBEAT;
	private Integer FREQUENCE;
	private Integer INTER_FREQ;
	private Float firstWalkingSpeed;
	private Boolean hasJumpEffect;
	private Integer firstJumpDuration;
	private Integer firstJumpAmplifier;
	private SonarHearingPlugin PLUGIN;

	public SneakyTask(SonarHearingPlugin plugin, Player player) {
		this.PLUGIN = plugin;
		this.PLAYER = player;
		this.HEARTBEAT = 0;
		this.FREQUENCE = 24;
		this.hasJumpEffect = false;
		this.firstJumpDuration = 0;
		this.firstJumpAmplifier = 0;
		this.firstWalkingSpeed = player.getWalkSpeed();
		if (plugin.FREEZE) {
			player.setWalkSpeed(0f);
			for (PotionEffect effect : player.getActivePotionEffects()) {
				if (effect.getType() == PotionEffectType.JUMP) {
					this.hasJumpEffect = true;
					this.firstJumpDuration = effect.getDuration();
					this.firstJumpAmplifier = effect.getAmplifier();
					player.removePotionEffect(PotionEffectType.JUMP);
				}
			}
			player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, -10, false, false));
		}
		this.INTER_FREQ = (this.FREQUENCE / 3);
		this.runTaskTimer(plugin, 0, 0);
	}

	public void run() {
		final Player player = this.PLAYER;
		if (player.isSneaking()) {
			reveal(player);
			if (this.PLUGIN.RED_ALERT)
				this.PLUGIN.INMS.redAlert(player, true);
			if (this.PLUGIN.HEARTBEAT) {
				HEARTBEAT++;
				if (HEARTBEAT >= this.FREQUENCE) {
					if (HEARTBEAT == this.FREQUENCE)
						heartbeat(player, true);
					if (HEARTBEAT > (this.FREQUENCE + this.INTER_FREQ)) {
						heartbeat(player, false);
						this.HEARTBEAT = 0;
					}
				}
			}
			return;
		}
		this.cancel();
		unreveal(player);
		if (this.PLUGIN.RED_ALERT)
			this.PLUGIN.INMS.redAlert(player, false);
		if (this.PLUGIN.FREEZE) {
			player.setWalkSpeed(firstWalkingSpeed);
			player.removePotionEffect(PotionEffectType.JUMP);
			if (hasJumpEffect)
				player.addPotionEffect(
						new PotionEffect(PotionEffectType.JUMP, this.firstJumpDuration, this.firstJumpAmplifier));
		}
	}

	public void reveal(final Player player) {
		for (final org.bukkit.entity.Entity entity : player.getNearbyEntities(this.PLUGIN.RADIUS, this.PLUGIN.RADIUS,
				this.PLUGIN.RADIUS)) {
			if (fr.roytreo.sonarhearing.core.handler.Entities.isRegistered(entity)) {
				Entities ent = fr.roytreo.sonarhearing.core.handler.Entities.getEntity(entity.getType());
				if (Utils.isReveable(entity)) {
					if (player.getLocation().distance(entity.getLocation()) <= ent.getRadius()) {
						if (!GlowAPI.isGlowing(entity, player)) {
							if (Utils.isTamed(entity) && this.PLUGIN.TAMED_MOB) {
								GlowAPI.setGlowing(entity, this.PLUGIN.TAMED_MOB_COLOR, player);
								continue;
							}
							if (entity instanceof Player) {
								if (Utils.getPlayerTeam(player.getScoreboard(), (Player) entity) != null) {
									playerTeam.put((Player) entity,
											Utils.getPlayerTeam(player.getScoreboard(), (Player) entity));
								} else {
									if (playerTeam.containsKey((Player) entity))
										playerTeam.remove((Player) entity);
								}
								GlowAPI.setGlowing(entity, ent.getColor(), player);
								continue;
							}
							GlowAPI.setGlowing(entity, ent.getColor(), player);
						}
					} else {
						if (GlowAPI.isGlowing(entity, player)) {
							if (entity instanceof Player) {
								GlowAPI.setGlowing(entity, false, player);
								if (playerTeam.containsKey((Player) entity)) {
									Team team = playerTeam.get((Player) entity);
									team.removeEntry(entity.getName());
									team.addEntry(entity.getName());
								}
								continue;
							}
							GlowAPI.setGlowing(entity, false, player);
						}
					}
				}
			}
		}
	}

	public void unreveal(final Player player) {
		for (final org.bukkit.entity.Entity entity : player.getNearbyEntities(this.PLUGIN.RADIUS, this.PLUGIN.RADIUS,
				this.PLUGIN.RADIUS)) {
			if (fr.roytreo.sonarhearing.core.handler.Entities.isRegistered(entity) && Utils.isReveable(entity)) {
				if (GlowAPI.isGlowing(entity, player)) {
					if (entity instanceof Player) {
						GlowAPI.setGlowing(entity, false, player);
						if (playerTeam.containsKey((Player) entity)) {
							Team team = playerTeam.get((Player) entity);
							team.removeEntry(entity.getName());
							team.addEntry(entity.getName());
						}
						continue;
					}
					GlowAPI.setGlowing(entity, false, player);
				}
			}
		}
	}

	public void heartbeat(Player player, Boolean effect) {
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BASEDRUM, 1.0f, 0.0f);
		if (effect) {
			PotionEffect potionEffect = new PotionEffect(PotionEffectType.SPEED, 10, 0, false, false);
			player.addPotionEffect(potionEffect);
		}
	}
}

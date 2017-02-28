package fr.asynchronous.sonarhearing.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import fr.asynchronous.sonarhearing.SonarHearingPlugin;

/**
 * @author Roytreo28
 */
public final class Utils {

	public static Boolean isReveable(Entity entity) {
		if (entity instanceof LivingEntity) {
			for (PotionEffect effect : ((LivingEntity) entity).getActivePotionEffects()) {
				if (effect.getType() == PotionEffectType.GLOWING)
					return false;
			}
		}
		return true;
	}

	public static Boolean isAgainstBlocks(Player player) {
		Location loc = player.getLocation();

		Block x1 = loc.clone().subtract(1, 0, 0).getBlock();
		Block x2 = loc.clone().add(1, 0, 0).getBlock();
		Block z1 = loc.clone().subtract(0, 0, 1).getBlock();
		Block z2 = loc.clone().add(0, 0, 1).getBlock();

		loc.add(0, 1, 0);
		Block X1 = loc.clone().subtract(1, 0, 0).getBlock();
		Block X2 = loc.clone().add(1, 0, 0).getBlock();
		Block Z1 = loc.clone().subtract(0, 0, 1).getBlock();
		Block Z2 = loc.clone().add(0, 0, 1).getBlock();

		if ((isValid(x1) && isValid(X1)) || (isValid(x2) && isValid(X2)) || (isValid(z1) && isValid(Z1))
				|| (isValid(z2) && isValid(Z2)))
			return true;
		return false;
	}

	public static Boolean isValid(Block block) {
		if (SonarHearingPlugin.blocklist.contains(block.getType()))
			return false;
		return true;
	}

	public static Boolean isTamed(Entity ent) {
		if (ent instanceof Tameable) {
			Tameable t = (Tameable) ent;
			if (t.isTamed())
				return true;
		}
		return false;
	}

	public static Team getPlayerTeam(Scoreboard board, Player player) {
		for (Team team : board.getTeams()) {
			for (String entry : team.getEntries()) {
				if (player.getName().equals(entry))
					return team;
			}
		}
		return null;
	}

	public static void refreshTeam(Player player, Player receiver) {
		if (getPlayerTeam(receiver.getScoreboard(), player) != null) {
			Team team = Utils.getPlayerTeam(receiver.getScoreboard(), player);
			team.removeEntry(player.getName());
			team.addEntry(player.getName());
		}
	}

	public static boolean isGlowing(Player player) {
		for (PotionEffect activePotionEffect : player.getActivePotionEffects()) {
			if (activePotionEffect.getType().equals(PotionEffectType.GLOWING))
				return true;
		}
		return false;
	}

	public static boolean inRange(int variable, int lowerBound, int higherBound) {
		if (variable <= higherBound && variable >= lowerBound)
			return true;
		return false;
	}
}
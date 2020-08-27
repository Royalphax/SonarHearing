package fr.royalpha.sonarhearing.v1_14_R1;

import fr.royalpha.sonarhearing.core.version.INMS;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import fr.royalpha.sonarhearing.core.util.ReflectionUtils;
import net.minecraft.server.v1_14_R1.PacketPlayOutWorldBorder;
import net.minecraft.server.v1_14_R1.WorldBorder;

/**
 * @author Royalphax
 */
public class NMS implements INMS {

	@Override
	public void redAlert(Player player, Boolean bool) {
		if (bool) {
			WorldBorder w = new WorldBorder();
			w.setSize(1.0D);
			w.setCenter(player.getLocation().getX() + 10000.0D, player.getLocation().getZ() + 10000.0D);
			PacketPlayOutWorldBorder packet = new PacketPlayOutWorldBorder();
			try {
				ReflectionUtils.setValue(packet, true, "a", PacketPlayOutWorldBorder.EnumWorldBorderAction.INITIALIZE);
				ReflectionUtils.setValue(packet, true, "c", w.getCenterX());
				ReflectionUtils.setValue(packet, true, "d", w.getCenterZ());
				ReflectionUtils.setValue(packet, true, "f", w.getSize());
				ReflectionUtils.setValue(packet, true, "e", w.k());
				ReflectionUtils.setValue(packet, true, "g", w.j());
				ReflectionUtils.setValue(packet, true, "b", w.m());
				ReflectionUtils.setValue(packet, true, "i", w.getWarningDistance());
				ReflectionUtils.setValue(packet, true, "h", w.getWarningTime());
				((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
			} catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			}
		} else {
			WorldBorder w = new WorldBorder();
			w.setSize(3.0E7D);
			w.setCenter(player.getLocation().getX(), player.getLocation().getZ());
			PacketPlayOutWorldBorder packet = new PacketPlayOutWorldBorder();
			try {
				ReflectionUtils.setValue(packet, true, "a", PacketPlayOutWorldBorder.EnumWorldBorderAction.INITIALIZE);
				ReflectionUtils.setValue(packet, true, "c", w.getCenterX());
				ReflectionUtils.setValue(packet, true, "d", w.getCenterZ());
				ReflectionUtils.setValue(packet, true, "f", w.getSize());
				ReflectionUtils.setValue(packet, true, "e", w.k());
				ReflectionUtils.setValue(packet, true, "g", w.j());
				ReflectionUtils.setValue(packet, true, "b", w.m());
				ReflectionUtils.setValue(packet, true, "i", w.getWarningDistance());
				ReflectionUtils.setValue(packet, true, "h", w.getWarningTime());
				((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
			} catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			}
		}
	}
}

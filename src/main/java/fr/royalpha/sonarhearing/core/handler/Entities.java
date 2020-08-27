package fr.royalpha.sonarhearing.core.handler;

import java.util.HashMap;

import org.bukkit.entity.EntityType;
import org.inventivetalent.glow.GlowAPI.Color;

/**
 * @author Royalpha
 */
public class Entities {

	public static HashMap<EntityType, Entities> map;

	static {
		map = new HashMap<>();
	}

	private EntityType TYPE;
	private Color COLOR;
	private Double RADIUS;
	private Boolean ENABLE;

	public Entities(EntityType type, Color color, Double radius, Boolean enable) {
		this.RADIUS = radius;
		this.TYPE = type;
		this.COLOR = color;
		this.ENABLE = enable;
		map.put(type, this);
	}

	public Color getColor() {
		return this.COLOR;
	}

	public Double getRadius() {
		return this.RADIUS;
	}

	public EntityType getType() {
		return this.TYPE;
	}

	public Boolean isEnable() {
		return this.ENABLE;
	}

	public static Double getMaxRadius() {
		Double radius = 0.0D;
		for (EntityType e : map.keySet()) {
			if (map.get(e).getRadius() > radius)
				radius = map.get(e).getRadius();
		}
		return radius;
	}

	public static Boolean isRegistered(org.bukkit.entity.Entity entity) {
		return (getEntity(entity.getType()) != null);
	}

	public static Entities getEntity(EntityType ent) {
		for (EntityType e : map.keySet()) {
			if (e == ent)
				return map.get(e);
		}
		return null;
	}
}

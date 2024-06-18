package com.github.echolightmc.msnametags;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class NameTag extends Entity {

	private static final Vec DEFAULT_TRANSLATION = new Vec(0, 0.15, 0);

	private final Entity owningEntity;
	private final TextDisplayMeta textMeta;

	NameTag(Entity owningEntity, boolean transparentBackground) {
		super(EntityType.TEXT_DISPLAY);
		this.owningEntity = owningEntity;
		hasPhysics = false;
		hasCollision = false;
		textMeta = getEntityMeta();
		textMeta.setNotifyAboutChanges(false);
		textMeta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.VERTICAL);
		textMeta.setSeeThrough(true);
		if (transparentBackground) {
			textMeta.setUseDefaultBackground(false);
			textMeta.setBackgroundColor(0);
		}
		textMeta.setTranslation(DEFAULT_TRANSLATION);
		textMeta.setNotifyAboutChanges(true);
		setAutoViewable(false);
		mount();
	}

	public Component getText() {
		return textMeta.getText();
	}

	public void setText(Component text) {
		textMeta.setText(text);
	}

	public Point getTranslation() {
		return textMeta.getTranslation();
	}

	public void setTranslation(Point point) {
		textMeta.setTranslation(point);
	}

	@Override
	public @NotNull TextDisplayMeta getEntityMeta() {
		return (TextDisplayMeta) super.getEntityMeta();
	}

	@Override
	public void tick(long time) { // don't do anything in super tick as it's unnecessary
		if (isRemoved()) return;
		for (Player player : getInstance().getPlayers()) {
			if (player == owningEntity) continue;
			Set<Player> viewers = getViewers();
			if (!viewers.contains(player) && (owningEntity.isViewer(player) && !owningEntity.isSneaking())) {
				revealTo(player);
			} else if (viewers.contains(player) && (!owningEntity.isViewer(player) || owningEntity.isSneaking())){
				hideFrom(player);
			}
		}
	}

	public void hideFrom(Player player) {
		removeViewer(player);
	}

	@SuppressWarnings("UnstableApiUsage")
	public void revealTo(Player player) {
		addViewer(player);
		player.sendPacket(NameTagManager.getPassengersPacket(owningEntity));
	}

	/**
	 * Effectively teleports this {@link NameTag} to the player and mounts it to the player.<br>
	 * Used during initialization and called if this {@link NameTag} isn't in the same
	 * {@link net.minestom.server.instance.Instance} as the owning player.
	 */
	public void mount() {
		setInstance(owningEntity.getInstance(), owningEntity.getPosition().asVec());
		owningEntity.addPassenger(this);
	}

}

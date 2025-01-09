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
		collidesWithEntities = false;
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

	/**
	 * @return the content of this nametag
	 */
	public Component getText() {
		return textMeta.getText();
	}

	/**
	 * @param text the new content of this nametag
	 */
	public void setText(Component text) {
		textMeta.setText(text);
	}

	/**
	 * @return the translation from the passenger point of the owner of this nametag to display this at
	 */
	public Point getTranslation() {
		return textMeta.getTranslation();
	}

	/**
	 * @param point the new translation from the passenger point of the owner of this nametag to display this at
	 */
	public void setTranslation(Point point) {
		textMeta.setTranslation(point);
	}

	/**
	 * @return the owner of this nametag (what this is riding)
	 */
	public Entity getOwningEntity() {
		return owningEntity;
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
				addViewer(player);
			} else if (viewers.contains(player) && (!owningEntity.isViewer(player) || owningEntity.isSneaking())){
				removeViewer(player);
			}
		}
	}

	@SuppressWarnings("UnstableApiUsage")
	@Override
	public void updateNewViewer(@NotNull Player player) {
		super.updateNewViewer(player);
		player.sendPacket(NameTagManager.getPassengersPacket(owningEntity)); // necessary otherwise it's not a passenger visually
	}

	/**
	 * Effectively teleports this {@link NameTag} to the player and mounts it to the player.<br>
	 * Used during initialization and called if this {@link NameTag} isn't in the same
	 * {@link net.minestom.server.instance.Instance} as the owning player.
	 */
	public void mount() {
		setInstance(owningEntity.getInstance(), owningEntity.getPosition()).whenComplete((unused, throwable) -> {
			if (throwable != null) throwable.printStackTrace();
			else owningEntity.addPassenger(this);
		});
	}

}

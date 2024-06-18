package com.github.echolightmc.msnametags;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityDespawnEvent;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.event.player.PlayerRespawnEvent;
import net.minestom.server.event.trait.EntityEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SetPassengersPacket;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class NameTagManager {

	private static final Tag<NameTag> NAME_TAG = Tag.Transient("msnametags.name-tag");

	private final Function<Entity, Team> teamCallback;

	@SuppressWarnings("UnstableApiUsage")
	public NameTagManager(EventNode<EntityEvent> node, Function<Entity, Team> teamCallback) {
		this.teamCallback = teamCallback;
		EventListener<PlayerRespawnEvent> respawnListener = EventListener.builder(PlayerRespawnEvent.class)
				.handler(event -> {
					Player player = event.getPlayer();
					if (!hasNameTag(player)) return;
					MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
						NameTag nameTag = getNameTag(player);
						if (nameTag != null) {
							nameTag.mount();
						}
					});
				})
				.build();
		node.addListener(respawnListener);
		EventListener<EntitySpawnEvent> spawnListener = EventListener.builder(EntitySpawnEvent.class)
				.handler(event -> {
					Entity entity = event.getEntity();
					Instance instance = event.getInstance();
					NameTag nameTag = getNameTag(entity);
					if (nameTag != null) {
						if (instance != nameTag.getInstance()) nameTag.mount();
					}
					// passengers fix for this player joining to see passengers of online players
					if (!(entity instanceof Player player)) return;
					for (Entity e : instance.getEntities()) {
						player.sendPacket(getPassengersPacket(e));
					}
				})
				.build();
		node.addListener(spawnListener);
		EventListener<EntityDespawnEvent> despawnListener = EventListener.builder(EntityDespawnEvent.class)
				.handler(event -> {
					Entity entity = event.getEntity();
					if (hasNameTag(entity)) entity.getTag(NAME_TAG).remove();
				})
				.build();
		node.addListener(despawnListener);
	}

	/**
	 * @param entity the entity to check to see if it has a nametag
	 * @return whether the provided entity has a nametag or not
	 */
	public boolean hasNameTag(Entity entity) {
		return entity.hasTag(NAME_TAG);
	}

	/**
	 * @param entity the entity to try to get the nametag of
	 * @return the nametag of the provided entity, or null
	 */
	public @Nullable NameTag getNameTag(Entity entity) {
		if (hasNameTag(entity)) return entity.getTag(NAME_TAG);
		return null;
	}

	/**
	 * Creates a nametag for the provided entity with a transparent background and automatically keeps it attached.
	 *
	 * @param entity the entity to create a nametag for
	 * @return the created nametag for the entity, or current nametag if it already exists
	 */
	public @NotNull NameTag createNameTag(Entity entity) {
		return createNameTag(entity, true);
	}

	/**
	 * Creates a nametag for the provided entity and automatically keeps it attached.
	 *
	 * @param entity the entity to create a nametag for
	 * @param transparentBackground whether the background of the nametag should be transparent or not
	 * @return the created nametag for the entity, or current nametag if it already exists
	 */
	@SuppressWarnings("DataFlowIssue") // getNameTag() can't be null here due to hasNameTag check
	public @NotNull NameTag createNameTag(Entity entity, boolean transparentBackground) {
		if (hasNameTag(entity)) return getNameTag(entity);
		NameTag nameTag = new NameTag(entity, transparentBackground);
		entity.setTag(NAME_TAG, nameTag);
		Team nameTagTeam = teamCallback.apply(entity);
		nameTagTeam.setNameTagVisibility(TeamsPacket.NameTagVisibility.NEVER);
		if (entity.getEntityType() == EntityType.PLAYER) {
			if (entity instanceof Player player) nameTagTeam.addMember(player.getUsername());
			else if (entity instanceof PlayerLikeEntity playerLikeEntity) nameTagTeam.addMember(playerLikeEntity.getUsername());
		} else nameTagTeam.addMember(entity.getUuid().toString());
		return nameTag;
	}

	/**
	 * {@link Entity#getPassengersPacket()}
	 *
	 * @param entity the entity to get the passengers of
	 * @return passengers packet of the provided entity
	 */
	@SuppressWarnings("JavadocReference")
	static SetPassengersPacket getPassengersPacket(Entity entity) {
		return new SetPassengersPacket(entity.getEntityId(),
				entity.getPassengers().stream().map(Entity::getEntityId).toList());
	}

}

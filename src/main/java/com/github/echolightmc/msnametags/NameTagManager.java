package com.github.echolightmc.msnametags;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityDespawnEvent;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.event.player.PlayerRespawnEvent;
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
	/**
	 * Tag to be set on entities which type is a player, so it can be properly added to its team.
	 */
	public static final Tag<String> USERNAME_TAG = Tag.String("msnametags.username");

	private final Function<Entity, Team> teamCallback;

	public NameTagManager(EventNode<Event> node, Function<Entity, Team> teamCallback) {
		this.teamCallback = teamCallback;
		EventListener<PlayerRespawnEvent> respawnListener = EventListener.builder(PlayerRespawnEvent.class)
				.handler(event -> {
					Player player = event.getPlayer();
					if (!hasNameTag(player)) return;
					MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
						NameTag nameTag = getNameTag(player);
						if (nameTag != null) nameTag.mount();
					});
				})
				.build();
		node.addListener(respawnListener);
		EventListener<EntitySpawnEvent> spawnListener = EventListener.builder(EntitySpawnEvent.class)
				.handler(event -> {
					Entity entity = event.getEntity();
					Instance instance = event.getInstance();
					NameTag nameTag = getNameTag(entity);
					if (nameTag != null && instance != nameTag.getInstance()) nameTag.mount();
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
		return createNameTag(entity, true, true);
	}

	/**
	 * Creates a nametag for the provided entity with a transparent background and automatically keeps it attached.
	 *
	 * @param entity the entity to create a nametag for
	 * @param transparentBackground whether the background of the nametag should be transparent or not
	 * @return the created nametag for the entity, or current nametag if it already exists
	 */
	public @NotNull NameTag createNameTag(Entity entity, boolean transparentBackground) {
		return createNameTag(entity, transparentBackground, true);
	}

	/**
	 * Creates a nametag for the provided entity and automatically keeps it attached.
	 *
	 * @param owner the entity to create a nametag for
	 * @param transparentBackground whether the background of the nametag should be transparent or not
	 * @param handleSneaking whether the nametag will be hidden if the owner is sneaking or not
	 * @return the created nametag for the entity, or current nametag if it already exists
	 */
	@SuppressWarnings("DataFlowIssue") // getNameTag() can't be null here due to hasNameTag check
	public @NotNull NameTag createNameTag(Entity owner, boolean transparentBackground, boolean handleSneaking) {
		if (hasNameTag(owner)) return getNameTag(owner);
		NameTag nameTag = new NameTag(owner, transparentBackground, handleSneaking);
		owner.setTag(NAME_TAG, nameTag);
		Team nameTagTeam = teamCallback.apply(owner);
		nameTagTeam.updateNameTagVisibility(TeamsPacket.NameTagVisibility.NEVER);
		if (owner.getEntityType() == EntityType.PLAYER) {
			if (owner instanceof Player player) nameTagTeam.addMember(player.getUsername());
			else if (owner.hasTag(USERNAME_TAG)) nameTagTeam.addMember(owner.getTag(USERNAME_TAG));
		} else nameTagTeam.addMember(owner.getUuid().toString());
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

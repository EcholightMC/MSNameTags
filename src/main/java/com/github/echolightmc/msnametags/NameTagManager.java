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

import java.util.function.Function;

public class NameTagManager {

	private static final Tag<NameTag> NAME_TAG = Tag.Transient("msnametags.name-tag");

	private final Function<Entity, Team> teamCallback;

	public NameTagManager(EventNode<Event> node, Function<Entity, Team> teamCallback) {
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

	public boolean hasNameTag(Entity entity) {
		return entity.hasTag(NAME_TAG);
	}

	public NameTag getNameTag(Entity entity) {
		if (hasNameTag(entity)) return entity.getTag(NAME_TAG);
		return null;
	}

	public @NotNull NameTag createNameTag(Entity entity) {
		return createNameTag(entity, true);
	}

	public @NotNull NameTag createNameTag(Entity entity, boolean transparentBackground) {
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

	static SetPassengersPacket getPassengersPacket(Entity entity) {
		return new SetPassengersPacket(entity.getEntityId(),
				entity.getPassengers().stream().map(Entity::getEntityId).toList());
	}

}

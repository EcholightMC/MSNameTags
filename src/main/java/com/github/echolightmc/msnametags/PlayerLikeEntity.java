package com.github.echolightmc.msnametags;

import net.minestom.server.entity.Entity;

/**
 * Important for team assignment to work properly in {@link NameTagManager#createNameTag(Entity, boolean)} as adding player
 * entities to a team requires a username, not a uuid like other entities.
 */
public interface PlayerLikeEntity {

	String getUsername();

}

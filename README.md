# MSNameTags
Simple nametag library for Minestom using text displays, handling the tracking and mounting for you.

## Usage
![Screenshot_11](https://github.com/EcholightMC/MSNameTags/assets/87914807/82365275-56e1-4aee-9c52-3c8d8da6ddaf)
```java
MiniMessage miniMessage = MiniMessage.miniMessage();
GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
TeamManager teamManager = MinecraftServer.getTeamManager();
Team nameTagTeam = new TeamBuilder("name-tags", teamManager)
						   .collisionRule(TeamsPacket.CollisionRule.NEVER)
						   .build();
NameTagManager nameTagManager = new NameTagManager(globalEventHandler, entity -> nameTagTeam);
globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
  Player player = event.getPlayer();
  NameTag playerNameTag = nameTagManager.createNameTag(player);
  playerNameTag.setText(miniMessage.deserialize("<red><b>OWNER</b> " + player.getUsername()));
  playerNameTag.addViewer(player); // add viewer to see own nametag, otherwise leave this out
  playerNameTag.mount(); // initial mount, required since we just added player as viewer otherwise nametag will appear stagnant for this player
});
```
## Add as Dependency
### Gradle
```gradle
repositories {
  ..
  maven {
    url = "https://maven.hapily.me/releases"
  }
}
```
```gradle
dependencies {
  ..
  implementation("com.github.echolightmc:MSNameTags:1.8-SNAPSHOT") {
    exclude group: "net.minestom", module: "minestom"
  }
}
```

## Troubleshooting

### [Player#setSkin](https://javadoc.minestom.net/net.minestom.server/net/minestom/server/entity/Player.html#setSkin%28net.minestom.server.entity.PlayerSkin%29)

When using `Player#setSkin`, you need to manually update the passengers (NameTags) list.
Due to a bug in the current **Minestom** implementation, passengers may get unmounted from the player after changing the skin.

This issue occurs on the client of the player whose skin is being updated.

After calling `setSkin`, resend the `SetPassengersPacket` to reattach passengers on the client.

```java
player.setSkin(skin);

player.sendPacket(new SetPassengersPacket(
    player.getEntityId(),
    player.getPassengers().stream()
        .map(Entity::getEntityId)
        .toList()
));
```

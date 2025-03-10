# MSNameTags
Simple nametag library for Minestom using text displays, handling the tracking and mounting for you.

## Usage
![Screenshot_11](https://github.com/EcholightMC/MSNameTags/assets/87914807/82365275-56e1-4aee-9c52-3c8d8da6ddaf)
```java
MiniMessage miniMessage = MiniMessage.miniMessage();
GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();
TeamManager teamManager = MinecraftServer.getTeamManager();
Team nameTagTeam = new TeamBuilder("name-tags", teamManager)
						   .collisionRule(TeamsPacket.CollisionRule.NEVER)
						   .build();
NameTagManager nameTagManager = new NameTagManager(eventHandler, entity -> nameTagTeam);
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
  implementation("com.github.echolightmc:MSNameTags:1.4-SNAPSHOT") {
    exclude group: "net.minestom", module: "minestom-snapshots"
  }
}
```

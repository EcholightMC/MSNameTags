import com.github.echolightmc.msnametags.NameTag;
import com.github.echolightmc.msnametags.NameTagManager;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.SharedInstance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.scoreboard.TeamBuilder;
import net.minestom.server.scoreboard.TeamManager;

void main() {
    MinecraftServer server = MinecraftServer.init();

    InstanceManager instanceManager = MinecraftServer.getInstanceManager();
    InstanceContainer container = instanceManager.createInstanceContainer();
    container.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
    SharedInstance sharedInstance = instanceManager.createSharedInstance(container);

    GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

    globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
        event.setSpawningInstance(container);
        event.getPlayer().setRespawnPoint(new Pos(0, 41, 0));
    });
    globalEventHandler.addListener(PlayerSwapItemEvent.class, event -> {
        Instance instance = event.getInstance();
        Player player = event.getPlayer();
        if (instance.equals(container)) player.setInstance(sharedInstance);
        else player.setInstance(container);
    });

    MiniMessage miniMessage = MiniMessage.miniMessage();
    TeamManager teamManager = MinecraftServer.getTeamManager();
    Team nameTagTeam = new TeamBuilder("name-tags", teamManager)
        .collisionRule(TeamsPacket.CollisionRule.NEVER)
        .teamColor(NamedTextColor.DARK_RED)
        .build();
    NameTagManager nameTagManager = new NameTagManager(globalEventHandler, entity -> nameTagTeam);
    globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
        Player player = event.getPlayer();
        player.setGlowing(true);
        NameTag playerNameTag = nameTagManager.createNameTag(player);
        playerNameTag.setText(miniMessage.deserialize("<red><b>OWNER</b> " + player.getUsername()));
        //playerNameTag.addViewer(player); // add viewer to see own nametag, otherwise leave this out
        playerNameTag.mount(); // initial mount, required since we just added player as viewer otherwise nametag will appear stagnant for this player
    });

    server.start("0.0.0.0", 25565);
}

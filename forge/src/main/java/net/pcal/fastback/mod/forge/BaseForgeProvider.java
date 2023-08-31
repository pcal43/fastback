package net.pcal.fastback.mod.forge;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.pcal.fastback.logging.SystemLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.LifecycleListener;
import net.pcal.fastback.mod.MinecraftProvider;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.mod.MinecraftProvider.messageToText;


/**
 * @author pcal
 * @since 0.16.0
 */
abstract class BaseForgeProvider implements MinecraftProvider {
    static final String MOD_ID = "fastback";

    private MinecraftServer logicalServer;
    private LifecycleListener lifecycleListener = null;
    private Runnable autoSaveListener;
    private boolean isWorldSaveEnabled;

    @Override
    public String getModVersion() {
        return "0.16.0+FIXME";
    }

    @Override
    public void setWorldSaveEnabled(boolean enabled) {
        for(ServerWorld world : logicalServer.getWorlds() ) {
            world.savingDisabled = !enabled;
        }
    }

    @Override
    public void saveWorld() {
        if (this.logicalServer == null) throw new IllegalStateException();
        this.logicalServer.saveAll(false, true, true); // suppressLogs, flush, force
    }

    @Override
    public void sendBroadcast(UserMessage userMessage) {
        if (this.logicalServer != null && this.logicalServer.isDedicated()) {
            logicalServer.getPlayerManager().broadcast(messageToText(userMessage), false);
        }
    }

    @Override
    public void setAutoSaveListener(Runnable runnable) {
        this.autoSaveListener = requireNonNull(runnable);
    }

    @Override
    public Path getSavesDir() {
        if (this.isClient()) {
            return logicalServer.getRunDirectory().toPath().resolve("saves");
        } else {
            return null;
        }
    }

    @Override
    public Path getWorldDirectory() {
        if (this.logicalServer == null) throw new IllegalStateException("minecraftServer is null");
        final LevelStorage.Session session = logicalServer.session;
        Path out = session.getWorldDir().toAbsolutePath().normalize();
        return out;
    }

    @Override
    public String getWorldName() {
        final LevelSummary ls = this.logicalServer.session.getLevelSummary();
        if (ls == null) return null;
        final LevelInfo li = ls.getLevelInfo();
        if (li == null) return null;
        return li.getLevelName();
    }

    /**
     * Add extra properties that will be stored in .fastback/backup.properties.
     */
    @Override
    public void addBackupProperties(Map<String, String> props) {
        props.put("fastback-version", this.getModVersion());
        if (this.logicalServer != null) {
            props.put("minecraft-version", logicalServer.getVersion());
            props.put("minecraft-game-mode", String.valueOf(logicalServer.getSaveProperties().getGameMode()));
            props.put("minecraft-level-name", logicalServer.getSaveProperties().getLevelName());
        }
    }

    /**
     * @return paths to the files and directories that should be backed up when config-backup is enabled.
     */
    @Override
    public Collection<Path> getModsBackupPaths() {
        final List<Path> out = new ArrayList<>();
        /**
        final FabricLoader fl = FabricLoader.getInstance();
        final Path gameDir = fl.getGameDir();
        out.add(gameDir.resolve("options.txt´"));
        out.add(gameDir.resolve("mods"));
        out.add(gameDir.resolve("config"));
        out.add(gameDir.resolve("resourcepacks"));
         **/
        return out;
    }

    // ======================================================================
    // Package private

    /**
     * This is the key initialization routine.  Registers the logger, the frameworkprovider and the commands
     * where the rest of the mod can get at them.
     */
    void onInitialize() {
        SystemLogger.Singleton.register(new Slf4jSystemLogger(LoggerFactory.getLogger(MOD_ID)));
        this.lifecycleListener = MinecraftProvider.register(this);
        syslog().debug("registered backup command");
        this.lifecycleListener.onInitialize();
        SshHacks.ensureSshSessionFactoryIsAvailable();
        syslog().info("Fastback initialized");
    }

    void onWorldStart(final MinecraftServer server) {
        setLogicalServer(server);
        requireNonNull(this.lifecycleListener).onWorldStart();
    }

    void onWorldStop() {
        requireNonNull(this.lifecycleListener).onWorldStop();
        setLogicalServer(null);
    }

    //FIXME!!
    void onAutoSaveComplete() {
        syslog().debug("onAutoSaveComplete");
        this.autoSaveListener.run();
    }

    abstract void renderOverlayText(DrawContext drawContext);

    // ======================================================================
    // Private

    private void setLogicalServer(MinecraftServer serverOrNull) {
        if ((serverOrNull == null) == (this.logicalServer == null)) throw new IllegalStateException();
        this.logicalServer = serverOrNull;
    }

}

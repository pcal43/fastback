package net.pcal.fastback.mod.forge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.pcal.fastback.logging.SystemLogger;
import net.pcal.fastback.mod.FrameworkServiceProvider;
import net.pcal.fastback.mod.LifecycleListener;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.SystemLogger.syslog;


/**
 * @author pcal
 * @since 0.16.0
 */
abstract class BaseForgeProvider implements FrameworkServiceProvider {
    static final String MOD_ID = "fastback";

    private MinecraftServer minecraftServer;
    private LifecycleListener lifecycleListener = null;
    private Runnable autoSaveListener;
    private boolean isWorldSaveEnabled;

    protected BaseForgeProvider() {}

    @Override
    public String getModVersion() {
        return "0.16.0+FIXME";
    }

    @Override
    public void setWorldSaveEnabled(boolean enabled) {
        this.isWorldSaveEnabled = enabled;
    }

    @Override
    public void saveWorld() {
        this.minecraftServer.saveAll(false, true, true); // suppressLogs, flush, force
    }

    @Override
    public void sendBroadcast(Text text) {
        if (this.minecraftServer != null && this.minecraftServer.isDedicated()) {
            minecraftServer.getPlayerManager().broadcast(text, false);
        }
    }

    @Override
    public void setAutoSaveListener(Runnable runnable) {
        this.autoSaveListener = requireNonNull(runnable);
    }

    @Override
    public Path getSavesDir() {
        if (this.isClient()) {
            return minecraftServer.getRunDirectory().toPath().resolve("saves");
        } else {
            return null;
        }
    }

    @Override
    public Path getWorldDirectory() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        final LevelStorage.Session session = minecraftServer.session;
        return session.directory.path();
    }

    @Override
    public String getWorldName() {
        final LevelSummary ls = this.minecraftServer.session.getLevelSummary();
        if (ls == null) return null;
        final LevelInfo li = ls.getLevelInfo();
        if (li == null) return null;
        return li.getLevelName();
    }

    /**
     * Add extra properties that will be stored in .fastback/backup.properties.
     */
    public void addBackupProperties(Map<String, String> props) {
        props.put("fastback-version", this.getModVersion());
        if (this.minecraftServer != null) {
            props.put("minecraft-version", minecraftServer.getVersion());
            props.put("minecraft-game-mode", String.valueOf(minecraftServer.getSaveProperties().getGameMode()));
            props.put("minecraft-level-name", minecraftServer.getSaveProperties().getLevelName());
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
        this.lifecycleListener = FrameworkServiceProvider.register(this);
        syslog().debug("registered backup command");
        hackJgitSsh();
        this.lifecycleListener.onInitialize();
        syslog().info("Fastback initialized");
    }

    void onWorldStart(final MinecraftServer server) {
        setMinecraftServer(server);
        requireNonNull(this.lifecycleListener).onWorldStart();
    }

    void onWorldStop() {
        setMinecraftServer(null);
        requireNonNull(this.lifecycleListener).onWorldStop();
    }


    // ======================================================================
    // Private

    private void setMinecraftServer(MinecraftServer serverOrNull) {
        if ((serverOrNull == null) == (this.minecraftServer == null)) throw new IllegalStateException();
        this.minecraftServer = serverOrNull;
    }


    private static void hackJgitSsh() {
        // This is necessary because JGit looks for it's SshSessionFactory with java.util.ServiceLoader, and that
        // just doesn't seem to be something that Forge is willing to accommodate.
        JschConfigSessionFactory csf = new JschConfigSessionFactory();
        SshSessionFactory.setInstance(csf);
        // This works fine but the jsch provider doesn't support ed25519 keys; for that we need to get mina
        // working, but that brings us into a whole other world of classloading and shading pain.  Because
        // Forge excludes everything under org.apache.*?  Is that true?  Ugh, FIXME.
        // This all works perfectly fine with Fabric. :(


        //https://stackoverflow.com/questions/67767455/setting-ssh-keys-to-use-with-jgit-with-ssh-from-apache-sshd
        //https://www.eclipse.org/forums/index.php/t/1107487/
        //https://github.com/AzBuilder/terrakube/blob/67f992c84cb2f66ce17a2e2ab85796872429720b/api/src/main/java/org/terrakube/api/plugin/ssh/TerrakubeSshdSessionFactory.java#L6

        //https://stackoverflow.com/questions/65566138/apache-mina-sshd-ssh-client-always-prints-eddsa-provider-not-supported
        //If you don't fix this, then you will not be able to validate the host keys. My testing was not impacted because I was not validating the host keys yet. However, once deployed to production, I would have been impacted because host keys must be validated.
        //https://dzone.com/articles/jgit-library-examples-in-java

        //SecurityUtils.setDefaultProviderChoice(new EdDSASecurityProviderRegistrar());

/**
 String CLAZZ = EdDSASecurityProviderRegistrar.class.getNa        this.lifecycleListener.onInitialize();
 me();//"net.pcal.fastback.relocated.org.apache.sshd.common.util.security.eddsa.EdDSASecurityProviderRegistrar";
 syslog().warn("!!!! "+CLAZZ);
 try {
 Class.forName(CLAZZ);
 } catch (ClassNotFoundException e) {
 syslog().error(new RuntimeException(e));
 }
 System.setProperty("org.apache.sshd.security.registrars", CLAZZ);
 **/
//


        /**
         *
         then we can try this, but i'm having a heck of a time getting the i2p provider to load >:(
         if (false) {
         File sshDir = new File(FS.DETECTED.userHome(), "/.ssh");

         SshdSessionFactory sshSessionFactory = new SshdSessionFactoryBuilder()
         .setPreferredAuthentications("publickey")
         .setHomeDirectory(FS.DETECTED.userHome())
         .setSshDirectory(sshDir)

         .setServerKeyDatabase((h, s) -> new ServerKeyDatabase() {

        @Override
        public List<PublicKey> lookup(String connectAddress,
        InetSocketAddress remoteAddress,
        Configuration config) {
        return Collections.emptyList();
        }

        @Override
        public boolean accept(String connectAddress,
        InetSocketAddress remoteAddress,
        PublicKey serverKey, Configuration config,
        CredentialsProvider provider) {
        return true;
        }

        })
         .build(new JGitKeyCache());
         SshSessionFactory.setInstance(sshSessionFactory);
         }
         **/
        /**

         //SshdSessionFactory factory = new SshdSessionFactory(new JGitKeyCache(), new DefaultProxyDataFactory());
         SshdSessionFactory factory = new SshdSessionFactory();
         try {
         Runtime.getRuntime()
         .addShutdownHook(new Thread(factory::close));
         } catch (IllegalStateException e) {
         // ignore - the VM is already shutting down
         }
         SshSessionFactory.setInstance(factory);
         **/

    }

}

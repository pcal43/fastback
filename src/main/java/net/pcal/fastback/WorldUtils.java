package net.pcal.fastback;

import net.pcal.fastback.ModContext.WorldContext;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

import static net.pcal.fastback.FileUtils.mkdirs;
import static net.pcal.fastback.LogUtils.debug;
import static net.pcal.fastback.ModConfig.Key.REPO_GIT_CONFIG;
import static net.pcal.fastback.FileUtils.writeResourceToFile;

@SuppressWarnings("FieldCanBeLocal")
public class WorldUtils {

    public static final Path WORLD_CONFIG_PATH = Path.of("fastback/fastback.properties");
    private static final String WORLD_CONFIG_RESOURCE = "world/fastback/fastback.properties";

    public static final Path WORLD_INFO_PATH = Path.of("fastback/world-info.properties");
    private static final String WORLD_UUID_PROPERTY = "world.uuid";

    private static final String GITIGNORE_RESOURCE = "world/fastback/dot-gitignore";
    private static final Path GITIGNORE_PATH = Path.of("fastback/.gitignore");

    public static void doWorldMaintenance(final ModConfig config, final WorldContext server, final ModContext.Logger logger)
            throws IOException, GitAPIException {
        final Path worldSaveDir = server.getWorldSaveDirectory();
        final Git git = Git.init().setDirectory(worldSaveDir.toFile()).call();
        final String rawConfig = config.get(REPO_GIT_CONFIG).replace(';', '\n');
        debug(logger, "updating local git config");
        GitUtils.mergeGitConfig(git, rawConfig, logger);
        updateWorldInfo(server, logger);
        writeResourceToFile(GITIGNORE_RESOURCE, worldSaveDir.resolve(GITIGNORE_PATH));
        updateDefaultWorldConfig(worldSaveDir);
    }

    private static void updateWorldInfo(final WorldContext world, final ModContext.Logger logger) throws IOException {
        final Path worldSaveDir = world.getWorldSaveDirectory();
        final Path worldPropsPath = worldSaveDir.resolve(WORLD_INFO_PATH);
        String worldUuid = null;
        if (worldPropsPath.toFile().exists()) {
            try {
                worldUuid = getWorldUuid(worldSaveDir);
            } catch (IOException ioe) {
                logger.warn("Unexpected problem looking up " + WORLD_UUID_PROPERTY +
                        ", will generate a new one ", ioe);
            }
        }
        if (worldUuid == null) worldUuid = UUID.randomUUID().toString();
        mkdirs(worldPropsPath.getParent());
        try (final FileWriter fw = new FileWriter(worldPropsPath.toFile());
             final PrintWriter out = new PrintWriter(fw)) {
            updateWorldInfo(world, worldUuid, out);
        }
    }

    public static String getWorldInfo(Path worldSaveDir) throws IOException {
        return Files.readString(worldSaveDir.resolve(WORLD_INFO_PATH));
    }

    public static void updateWorldInfo(WorldContext world, String worldUuid, PrintWriter out) {
        out.println(WORLD_UUID_PROPERTY + "         = " + worldUuid);
        out.println("world.name         = " + world.getWorldName());
        out.println("world.seed         = " + world.getSeed());
        out.println("world.mode         = " + world.getGameMode());
        out.println("world.difficulty   = " + world.getDifficulty());
        out.println("minecraft.version  = " + world.getMinecraftVersion());
        out.println("fastback.version   = " + world.getModContext().getFastbackModVersion());
    }

    public static String getWorldUuid(Path worldSaveDir) throws IOException {
        final Path worldInfoPath = worldSaveDir.resolve(WORLD_INFO_PATH);
        final Properties worldProps = new Properties();
        try (final FileReader in = new FileReader(worldInfoPath.toFile())) {
            worldProps.load(in);
        }
        final String uuid = worldProps.getProperty(WORLD_UUID_PROPERTY);
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new IOException(WORLD_UUID_PROPERTY + " is unexpectedly " + uuid + " in " + worldInfoPath);
        }
        return uuid;
    }

    private static void updateDefaultWorldConfig(final Path worldSaveDir) throws IOException {
        final Path worldConfigPath = worldSaveDir.resolve(WORLD_CONFIG_PATH);
        if (!worldConfigPath.toFile().exists()) {
            writeResourceToFile(WORLD_CONFIG_RESOURCE, worldConfigPath);
        }
    }
}

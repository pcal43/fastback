package net.pcal.fastback;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static net.pcal.fastback.ModConfig.Key.REPO_GIT_CONFIG;
import static net.pcal.fastback.FileUtils.writeResourceToFile;

@SuppressWarnings("FieldCanBeLocal")
public class WorldUtils {

    public static final Path WORLD_CONFIG_PATH = Path.of("fastback/fastback.properties");
    private static final String WORLD_CONFIG_RESOURCE = "world/fastback/fastback.properties";

    private static final Path WORLD_UUID_PATH = Path.of("world.uuid");

    private static final Iterable<Pair<String, Path>> WORLD_RESOURCES_TO_COPY = List.of(
            Pair.of("world/fastback/dot-gitignore", Path.of("fastback/.gitignore")),
            Pair.of("world/dot-gitignore", Path.of(".gitignore"))
    );

    public static void doWorldMaintenance(final ModConfig configKillme, final Path worldSaveDir, final Loggr logger)
            throws IOException, GitAPIException {
        final Git git = Git.init().setDirectory(worldSaveDir.toFile()).call();
        final String rawConfig = configKillme.get(REPO_GIT_CONFIG).replace(';', '\n');
        logger.debug("updating local git config");
        GitUtils.mergeGitConfig(git, rawConfig, logger);
        ensureWorldHasUuid(worldSaveDir, logger);
        for (final Pair<String, Path> resource2path : WORLD_RESOURCES_TO_COPY) {
            writeResourceToFile(resource2path.getLeft(), worldSaveDir.resolve(resource2path.getRight()));
        }
        updateDefaultWorldConfig(worldSaveDir);
    }

    private static void ensureWorldHasUuid(final Path worldSaveDir, final Loggr logger) throws IOException {
        final Path worldUuidpath = worldSaveDir.resolve(WORLD_UUID_PATH);
        if (!worldUuidpath.toFile().exists()) {
            final String newUuid = UUID.randomUUID().toString();
            try (final FileWriter fw = new FileWriter(worldUuidpath.toFile())) {
                fw.append(newUuid);
                fw.append('\n');
            }
            logger.info("Generated new world.uuid " + newUuid);
        }
    }

    public static String getWorldUuid(Path worldSaveDir) throws IOException {
        return Files.readString(worldSaveDir.resolve(WORLD_UUID_PATH)).trim();
    }

    private static void updateDefaultWorldConfig(final Path worldSaveDir) throws IOException {
        final Path worldConfigPath = worldSaveDir.resolve(WORLD_CONFIG_PATH);
        if (!worldConfigPath.toFile().exists()) {
            writeResourceToFile(WORLD_CONFIG_RESOURCE, worldConfigPath);
        }
    }
}

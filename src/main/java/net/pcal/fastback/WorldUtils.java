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

import static net.pcal.fastback.FileUtils.writeResourceToFile;

@SuppressWarnings("FieldCanBeLocal")
public class WorldUtils {

    private static final Path WORLD_UUID_PATH = Path.of("world.uuid");

    private static final Iterable<Pair<String, Path>> WORLD_RESOURCES_TO_COPY = List.of(
            Pair.of("world/dot-gitignore", Path.of(".gitignore"))
    );

    public static void doWorldMaintenance(final Path worldSaveDir, final Loggr logger) throws IOException {
        if (worldSaveDir.resolve(".git").toFile().exists()) {
            logger.info("Doing world maintenance");
            // final Git git = Git.init().setDirectory(worldSaveDir.toFile()).call();  FIXME do configuration?
            ensureWorldHasUuid(worldSaveDir, logger);
            for (final Pair<String, Path> resource2path : WORLD_RESOURCES_TO_COPY) {
                writeResourceToFile(resource2path.getLeft(), worldSaveDir.resolve(resource2path.getRight()));
            }
        } else {
            logger.debug("backups not enabled for world, skipping maintenance");
        }
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

}

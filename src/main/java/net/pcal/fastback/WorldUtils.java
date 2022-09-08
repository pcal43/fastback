package net.pcal.fastback;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static net.pcal.fastback.FileUtils.writeResourceToFile;
import static net.pcal.fastback.WorldConfig.ensureWorldHasUuid;

@SuppressWarnings("FieldCanBeLocal")
public class WorldUtils {

    private static final Iterable<Pair<String, Path>> WORLD_RESOURCES_TO_COPY = List.of(
            Pair.of("world/dot-gitignore", Path.of(".gitignore")),
            Pair.of("world/dot-gitattributes", Path.of(".gitattributes"))
    );

    public static void doWorldMaintenance(final Path worldSaveDir, final Loggr logger) throws IOException {
        if (worldSaveDir.resolve(".git").toFile().exists()) {
            logger.info("Doing world maintenance");
            ensureWorldHasUuid(worldSaveDir, logger);
            for (final Pair<String, Path> resource2path : WORLD_RESOURCES_TO_COPY) {
                writeResourceToFile(resource2path.getLeft(), worldSaveDir.resolve(resource2path.getRight()));
            }
            try (final Git git = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
                final StoredConfig config = git.getRepository().getConfig();
                config.setInt("core", null, "compression", 0);
                config.setInt("pack", null, "window", 0);
                config.save();
            } catch (GitAPIException e) {
                logger.error("failed to do world maintenance", e);
            }

        } else {
            logger.debug("backups not enabled for world, skipping maintenance");
        }
    }
}

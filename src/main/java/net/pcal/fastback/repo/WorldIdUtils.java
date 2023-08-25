package net.pcal.fastback.repo;

import net.pcal.fastback.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static net.pcal.fastback.logging.SystemLogger.syslog;

/**
 * Utils for managing the world.uuid file, which uniquely identifies a given world
 * for backup purposes.  The basic idea is we want to help them avoid mixing snapshots
 * from different worlds in the same remote repository, since that will be painful to
 * untangle and be pretty inefficient.
 *
 * @author pcal
 * @since 0.13.0
 */
class WorldIdUtils {

    // ======================================================================
    // Constants

    static final Path WORLD_UUID_PATH = Path.of(".fastback/world.uuid");

    // ======================================================================
    // Utils

     static String getWorldUuid(final Path worldSaveDir) throws IOException {
         migrateFastbackDir(worldSaveDir);
         final Path uuidPath = worldSaveDir.resolve(WORLD_UUID_PATH);
         if (!uuidPath.toFile().exists()) throw new FileNotFoundException(uuidPath.toString());
         return Files.readString(uuidPath).trim();
    }

    static void createWorldUuid(final Path worldSaveDir) throws IOException {
        migrateFastbackDir(worldSaveDir);
        final Path worldUuidpath = worldSaveDir.resolve(WORLD_UUID_PATH).toAbsolutePath().normalize();
        FileUtils.mkdirs(worldUuidpath.getParent());
        final String newUuid = UUID.randomUUID().toString();
        try (final FileWriter fw = new FileWriter(worldUuidpath.toFile())) {
            fw.append(newUuid);
            fw.append('\n');
        }
        syslog().debug("Generated new world.uuid " + newUuid);
    }

    static void ensureWorldHasUuid(final Path worldSaveDir) throws IOException {
        migrateFastbackDir(worldSaveDir);
        final Path worldUuidpath = worldSaveDir.resolve(WORLD_UUID_PATH).toAbsolutePath().normalize();
        if (!worldUuidpath.toFile().exists()) {
            syslog().warn("Did not find expected uuid file at "+worldUuidpath);
            syslog().warn("We'll create a new one and carry on.  But this indicates something weird is going on.");
            createWorldUuid(worldSaveDir);
        }
    }

    // ======================================================================
    // Private

    private static void migrateFastbackDir(final Path worldSaveDir) {
        final File oldDir = worldSaveDir.resolve("fastback").toAbsolutePath().normalize().toFile();
        if (oldDir.exists()) {
            final File newDir = worldSaveDir.resolve(".fastback").toAbsolutePath().normalize().toFile();
            if (!newDir.exists()) {
                if (oldDir.renameTo(newDir)) {
                    syslog().info("moved "+oldDir + " to " + newDir);
                } else {
                    syslog().error("failed to move "+oldDir + " to " + newDir);
                }
            }
        }
    }

}

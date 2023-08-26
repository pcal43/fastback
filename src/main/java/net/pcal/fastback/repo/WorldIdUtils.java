package net.pcal.fastback.repo;

import net.pcal.fastback.repo.SnapshotIdUtils.SnapshotIdCodec;
import net.pcal.fastback.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.SystemLogger.syslog;

/**
 * Utils for managing the world.id file, which uniquely identifies a given world
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

    /**
     * Path to where we store a short, unique-ish identifier for this world
     */
    private static final Path WORLD_ID_PATH = Path.of(".fastback/world-id");

    /**
     * Character length of randomly-generated world id's.  58^4 seems good
     * enough for our purposes.
     */
    private static final int WORLD_ID_LENGTH = 4;

    /**
     * Path where we used to store the world id.  (pre-0.15)
     */
    @Deprecated
    private static final Path OLD_WORLD_UUID_PATH = Path.of(".fastback/world.uuid");

    // ======================================================================
    // Utils

    record WorldIdInfo(WorldId wid, SnapshotIdCodec sidCodec) {
    }

    /**
     * @return the WorldId and the SnapshotIdCodec to use.  Never returns null (though the worldId might be null).
     */
    static WorldIdInfo getWorldIdInfo(final Path worldSaveDir) throws IOException {
        migrateFastbackDir(worldSaveDir);
        {
            final Path idPath = worldSaveDir.resolve(WORLD_ID_PATH);
            if (idPath.toFile().exists()) {
                final WorldId wid = new WorldIdImpl(requireNonNull(Files.readString(idPath).trim()));
                return new WorldIdInfo(wid, SnapshotIdCodec.V2);
            }
        }
        {
            final Path uuidPath = worldSaveDir.resolve(OLD_WORLD_UUID_PATH);
            if (uuidPath.toFile().exists()) {
                final WorldId wid = new WorldIdImpl(requireNonNull(Files.readString(uuidPath).trim()));
                return new WorldIdInfo(wid, SnapshotIdCodec.V1);
            }

        }
        throw new FileNotFoundException(WORLD_ID_PATH.toString());
    }

    static void createWorldId(final Path worldSaveDir) throws IOException {
        migrateFastbackDir(worldSaveDir);
        final Path worldIdPath = worldSaveDir.resolve(WORLD_ID_PATH).toAbsolutePath().normalize();
        FileUtils.mkdirs(worldIdPath.getParent());
        final String worldId = generateRandomWorldId(WORLD_ID_LENGTH);
        try (final FileWriter fw = new FileWriter(worldIdPath.toFile())) {
            fw.append(worldId);
            fw.append('\n');
        }
        syslog().debug("Wrote new worldId " + worldId + " to " + worldIdPath);
    }

    static void ensureWorldHasId(final Path worldSaveDir) throws IOException {
        migrateFastbackDir(worldSaveDir);
        final Path worldIdPath = worldSaveDir.resolve(WORLD_ID_PATH).toAbsolutePath().normalize();
        if (!worldIdPath.toFile().exists()) {
            syslog().warn("Did not find expected id file at " + worldIdPath);
            syslog().warn("We'll create a new one and carry on.  But this indicates something weird is going on.");
            createWorldId(worldSaveDir);
        }
    }

    record WorldIdImpl(String id) implements WorldId {
        @Override
        public String toString() {
            return id;
        }
    }

    // ======================================================================
    // Exposed for unit-testing

    static String generateRandomWorldId(int length) {
        final StringBuilder out = new StringBuilder();
        final Random r = new Random();
        for (int i = 0; i < length; i++) {
            out.append(BASE58_CHARS[r.nextInt(BASE58_CHARS.length)]);
        }
        return out.toString();
    }

    // ======================================================================
    // Private

    private static final char[] BASE58_CHARS =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

    /**
     * Generate a random ID string of the given length.
     */
    private static void migrateFastbackDir(final Path worldSaveDir) {
        final File oldDir = worldSaveDir.resolve("fastback").toAbsolutePath().normalize().toFile();
        if (oldDir.exists()) {
            final File newDir = worldSaveDir.resolve(".fastback").toAbsolutePath().normalize().toFile();
            if (!newDir.exists()) {
                if (oldDir.renameTo(newDir)) {
                    syslog().info("moved " + oldDir + " to " + newDir);
                } else {
                    syslog().error("failed to move " + oldDir + " to " + newDir);
                }
            }
        }
    }

}

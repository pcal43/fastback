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
 *
 * @author pcal
 * @since 0.13.0
 */
class UuidUtils {

    // ======================================================================
    // Constants

    static final Path WORLD_UUID_PATH = Path.of(".fastback/world.uuid");
    static final Path WORLD_UUID_PATH_OLD = Path.of("fastback/world.uuid");

    // ======================================================================
    // Utils

     static String getWorldUuid(final Path worldSaveDir) throws IOException {
        Path uuidPath = worldSaveDir.resolve(UuidUtils.WORLD_UUID_PATH);
        if (!uuidPath.toFile().exists()) {
            uuidPath = worldSaveDir.resolve(UuidUtils.WORLD_UUID_PATH_OLD);
            if (!uuidPath.toFile().exists()) {
                throw new FileNotFoundException(uuidPath.toString());
            } else {
                syslog().warn("Unexpectedly loading from "+WORLD_UUID_PATH_OLD);
            }
        }
        return Files.readString(uuidPath).trim();
    }

    static void ensureWorldHasUuid(final Path worldSaveDir) throws IOException {
        final Path worldUuidpath = worldSaveDir.resolve(WORLD_UUID_PATH).toAbsolutePath().normalize();
        if (!worldUuidpath.toFile().exists()) {
            // see if they have one at the old location
            final Path worldUuidpathOld = worldSaveDir.resolve(WORLD_UUID_PATH_OLD).toAbsolutePath().normalize();
            if (worldUuidpathOld.toFile().exists()) {
                FileUtils.mkdirs(worldUuidpath.getParent());
                org.apache.commons.io.FileUtils.moveFile(worldUuidpathOld.toFile(), worldUuidpath.toFile());
                final Path oldParent = worldUuidpathOld.getParent();
                if (org.apache.commons.io.FileUtils.isEmptyDirectory(oldParent.toFile())) {
                    FileUtils.rmdir(oldParent);
                }
                syslog().info("moved "+worldUuidpathOld + " to " + worldUuidpath);
            } else {
                syslog().warn("Did not find expected uuid file at " + worldUuidpath);
                syslog().warn("We'll create a new one and carry on.  But this indicates something weird is going on.");
                createWorldUuid(worldSaveDir);
            }
        }
    }

    static void createWorldUuid(final Path worldSaveDir) throws IOException {
        final Path worldUuidpath = worldSaveDir.resolve(WORLD_UUID_PATH).toAbsolutePath().normalize();
        FileUtils.mkdirs(worldUuidpath.getParent());
        final String newUuid = UUID.randomUUID().toString();
        try (final FileWriter fw = new FileWriter(worldUuidpath.toFile())) {
            fw.append(newUuid);
            fw.append('\n');
        }
        syslog().debug("Generated new world.uuid " + newUuid);
    }

}

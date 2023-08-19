/*
 * FastBack - Fast, incremental Minecraft backups powered by Git.
 * Copyright (C) 2022 pcal.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package net.pcal.fastback.config;

import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.FileUtils;
import org.eclipse.jgit.api.Git;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static net.pcal.fastback.config.GitConfigKey.UPDATE_GITATTRIBUTES_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.UPDATE_GITIGNORE_ENABLED;
import static net.pcal.fastback.utils.FileUtils.writeResourceToFile;

/**
 * Assorted crap that ended up homeless after config refactoring.  Will
 * clean this up as well eventually.
 *
 * @author pcal
 */
//@Deprecated
public class RepoConfigUtils {

    public static final Path WORLD_UUID_PATH = Path.of("fastback/world.uuid");

    public static String getWorldUuid(Git git) throws IOException {
        return Files.readString(git.getRepository().getWorkTree().toPath().
                toAbsolutePath().resolve(WORLD_UUID_PATH)).trim();
    }

    public static void ensureWorldHasUuid(final Path worldSaveDir, final Logger logger) throws IOException {
        final Path worldUuidpath = worldSaveDir.resolve(WORLD_UUID_PATH);
        if (!worldUuidpath.toFile().exists()) {
            FileUtils.mkdirs(worldUuidpath.getParent());
            final String newUuid = UUID.randomUUID().toString();
            try (final FileWriter fw = new FileWriter(worldUuidpath.toFile())) {
                fw.append(newUuid);
                fw.append('\n');
            }
            logger.info("Generated new world.uuid " + newUuid);
        }
    }


    // ====================================================================
    // Resource management

    private record WorldResource(
            String resourcePath, // Note to self: needs to be a String, not a Path, because Windows slashes don't work
            Path targetPath,
            GitConfigKey permission
    ) {}

    private static final Iterable<WorldResource> WORLD_RESOURCES = List.of(
            new WorldResource(
                    "world/dot-gitignore",
                    Path.of(".gitignore"),
                    UPDATE_GITIGNORE_ENABLED),
            new WorldResource(
                    "world/dot-gitattributes",
                    Path.of(".gitattributes"),
                    UPDATE_GITATTRIBUTES_ENABLED)
    );

    public static void doWorldMaintenance(final Git jgit, final Logger logger) throws IOException, IOException {
        logger.info("Doing world maintenance");
        final Path worldSaveDir = jgit.getRepository().getWorkTree().toPath();
        ensureWorldHasUuid(worldSaveDir, logger);
        final GitConfig config = GitConfig.load(jgit);
        for (final WorldResource resource : WORLD_RESOURCES) {
            if (config.getBoolean(resource.permission)) {
                logger.debug("Updating " + resource.targetPath);
                final Path targetPath = worldSaveDir.resolve(resource.targetPath);
                writeResourceToFile(resource.resourcePath, targetPath);
            } else {
                logger.info("Updates disabled for " + resource.targetPath);
            }
        }
    }
}

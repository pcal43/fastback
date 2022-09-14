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

package net.pcal.fastback;

import net.pcal.fastback.logging.Logger;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static net.pcal.fastback.WorldConfig.ensureWorldHasUuid;
import static net.pcal.fastback.utils.FileUtils.writeResourceToFile;

@SuppressWarnings("FieldCanBeLocal")
public class WorldUtils {

    private static final Iterable<Pair<String, Path>> WORLD_RESOURCES_TO_COPY = List.of(
            Pair.of("world/dot-gitignore", Path.of(".gitignore")),
            Pair.of("world/dot-gitattributes", Path.of(".gitattributes"))
    );

    public static void doWorldMaintenance(final Git git, final Logger logger) throws IOException {
        logger.info("Doing world maintenance");
        final Path worldSaveDir = git.getRepository().getWorkTree().toPath();
        ;
        ensureWorldHasUuid(worldSaveDir, logger);
        for (final Pair<String, Path> resource2path : WORLD_RESOURCES_TO_COPY) {
            writeResourceToFile(resource2path.getLeft(), worldSaveDir.resolve(resource2path.getRight()));
        }
        final StoredConfig config = git.getRepository().getConfig();
        config.setInt("core", null, "compression", 0);
        config.setInt("pack", null, "window", 0);
        config.save();
    }
}
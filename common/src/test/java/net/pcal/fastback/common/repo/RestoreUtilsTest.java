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

package net.pcal.fastback.common.repo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestoreUtilsTest {

    @Test
    void removesPathSeparatorsFromWorldName(@TempDir Path restoreDirectory) {
        assertEquals(
                restoreDirectory.resolve("MyWorld-snapshot"),
                RestoreUtils.getTargetDir(restoreDirectory, "My/World", "snapshot")
        );
        assertEquals(
                restoreDirectory.resolve("MyWorld-snapshot"),
                RestoreUtils.getTargetDir(restoreDirectory, "My:World", "snapshot")
        );
        assertEquals(
                restoreDirectory.resolve("MyWorld-snapshot"),
                RestoreUtils.getTargetDir(restoreDirectory, "My\\World", "snapshot")
        );
    }

    @Test
    void preservesPlainEnglishWorldName(@TempDir Path restoreDirectory) {
        Path target = RestoreUtils.getTargetDir(
                restoreDirectory,
                "My World",
                "snapshot"
        );

        assertEquals(
                restoreDirectory.resolve("MyWorld-snapshot"),
                target
        );
    }

    @Test
    void preservesNumbersInWorldName(@TempDir Path restoreDirectory) {
        Path target = RestoreUtils.getTargetDir(
                restoreDirectory,
                "World 123",
                "snapshot"
        );

        assertEquals(
                restoreDirectory.resolve("World123-snapshot"),
                target
        );
    }

    @Test
    void preservesChineseWorldName(@TempDir Path restoreDirectory) {
        Path target = RestoreUtils.getTargetDir(
                restoreDirectory,
                "我的世界",
                "snapshot"
        );

        assertEquals(
                restoreDirectory.resolve("我的世界-snapshot"),
                target
        );
    }
}

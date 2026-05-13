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
package net.pcal.fastback.common.mixins;

import net.minecraft.util.filefix.FileFixerUpper;
import net.minecraft.util.filefix.virtualfilesystem.CopyOnWriteFileSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * Prevents MC 26.1+'s CopyOnWriteFileSystem (used during world upgrade) from choking
 * on read-only git object files inside the .git directory.
 *
 * @author pcal
 * @since 0.31.2
 */
@Mixin(FileFixerUpper.class)
public class FileFixerUpperMixin {

    @Redirect(
        method = "applyFileFixersOnCow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/filefix/virtualfilesystem/CopyOnWriteFileSystem;create(Ljava/lang/String;Ljava/nio/file/Path;Ljava/nio/file/Path;Ljava/nio/file/PathMatcher;)Lnet/minecraft/util/filefix/virtualfilesystem/CopyOnWriteFileSystem;",
            remap = false
        ),
        remap = false
    )
    private CopyOnWriteFileSystem fastback_skipGitDir(
            String name, Path baseDir, Path tmpDir, PathMatcher original) throws IOException {
        PathMatcher withGitSkip = path -> {
            for (Path component : path) {
                if (".git".equals(component.toString())) return true;
            }
            return original.matches(path);
        };
        return CopyOnWriteFileSystem.create(name, baseDir, tmpDir, withGitSkip);
    }
}


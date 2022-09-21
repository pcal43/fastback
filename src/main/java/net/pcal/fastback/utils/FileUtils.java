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

package net.pcal.fastback.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

public class FileUtils {

    public static void mkdirs(final Path path) throws IOException {
        final File file = path.toFile();
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IOException("Cannot create directory because file exists at " + path);
            }
        } else {
            file.mkdirs();
            if (!file.exists() || !file.isDirectory()) {
                throw new IOException("Failed to create directory at " + path);
            }
        }
    }

    public static void rmdir(final Path path) throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(path.toFile());
    }

    public static String getDirDisplaySize(File dir) {
        final long gitDirSize = org.apache.commons.io.FileUtils.sizeOfDirectory(dir);
        return org.apache.commons.io.FileUtils.byteCountToDisplaySize(gitDirSize);
    }

    public static void writeResourceToFile(String resourcePath, Path targetFile) throws IOException {
        final String rawResource;
        try (InputStream in = FileUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Unable to load resource " + resourcePath); // wat
            }
            rawResource = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        mkdirs(targetFile.getParent());
        Files.writeString(targetFile, rawResource);
    }
}

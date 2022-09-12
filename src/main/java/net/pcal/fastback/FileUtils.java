package net.pcal.fastback;

import net.pcal.fastback.fabric.FastbackInitializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public static void writeResourceToFile(String resourcePath, Path targetFile) throws IOException {
        final String rawResource;
        try (InputStream in = FastbackInitializer.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Unable to load resource " + resourcePath); // wat
            }
            rawResource = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        mkdirs(targetFile.getParent());
        Files.writeString(targetFile, rawResource);
    }
}

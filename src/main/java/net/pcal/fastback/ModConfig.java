package net.pcal.fastback;

import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.LogUtils.debug;
import static net.pcal.fastback.WorldUtils.WORLD_CONFIG_PATH;

public class ModConfig {

    private static final Path MOD_CONFIG_PATH = Paths.get("config", "fastback.properties");
    private static final String DEFAULT_MOD_CONFIG_RESOURCE = "config/fastback.properties";
    private static final String DEFAULT_PROPERTIES_RESOURCE = "/config-defaults.properties";

    public enum Key {

        FASTBACK_ENABLED("fastback.enabled"),

        PUSH_ENABLED("push.enabled"),
        PUSH_REMOTE_NAME("push.remote-name"),
        PUSH_UUID_CHECK_ENABLED("push.uuid-check-enabled"),
        PUSH_UUID_CHECK_PREFIX("push.uuid-check-branch-name-prefix"),

        REPO_GIT_CONFIG("repo.git-config"),
        REPO_LATEST_BRANCH_NAME("repo.latest-branch-name"),


        SMART_PUSH_ENABLED("smart-push.enabled"),
        SMART_PUSH_BRANCH_NAME("smart-push.last-push-branch-name"),
        SMART_PUSH_TEMP_BRANCH_FORMAT("smart-push.temp-branch-format"),
        SMART_PUSH_TEMP_BRANCH_CLEANUP_ENABLED("smart-push.temp-branch-cleanup-enabled"),
        SMART_PUSH_REMOTE_TEMP_BRANCH_CLEANUP_ENABLED("smart-push.remote-temp-branch-cleanup-enabled"),

        REMOTE_UPSTREAM_ENABLED("remote-upstream.enabled"),
        REMOTE_UPSTREAM_URI("remote-upstream.uri"),

        FILE_UPSTREAM_ENABLED("file-upstream.enabled"),
        FILE_UPSTREAM_PATH("file-upstream.path"),
        FILE_UPSTREAM_BARE("file-upstream.bare"),
        FILE_UPSTREAM_GIT_CONFIG("file-upstream.git-config"),

        SHUTDOWN_BACKUP_ENABLED("shutdown-backup.enabled");

//        SCHEDULE_ENABLED("scheduled-backup.enabled"),
//        SCHEDULE_CRON("scheduled-backup.cron"),

        private final String propertyName;

        Key(String propertyName) {
            this.propertyName = requireNonNull(propertyName);
        }

        public String getPropertyName() {
            return this.propertyName;
        }
    }

    private final Properties properties;

    public ModConfig(final Properties props) {
        this.properties = requireNonNull(props);
    }

    public String get(Key key) {
        return this.properties.getProperty(key.propertyName);
    }

    public Boolean getBoolean(Key key) {
        return Boolean.parseBoolean(get(key));
    }

    int getInt(Key key) {
        return Integer.parseInt(get(key));
    }

    // ===================================================================
    // Static utilities

    /**
     * Load the mod configuration.  This is used when no world is open.
     */
    public static ModConfig load(final Logger logger) throws IOException {
        final Properties props = new Properties();
        loadDefaultProperties(props);
        loadFileProperties(props, MOD_CONFIG_PATH);
        return new ModConfig(props);
    }

    /**
     * Load the mod configuration.  This is used when no world is open.
     */
    public static ModConfig loadForWorld(final Path worldSaveDir, final Logger logger) throws IOException {
        final Properties props = new Properties();
        loadDefaultProperties(props);
        loadFileProperties(props, MOD_CONFIG_PATH);
        final Path worldConfigPath = worldSaveDir.resolve(WORLD_CONFIG_PATH);
        // Load the mod configuration.
        if (!worldConfigPath.toFile().exists()) {
            debug(logger, () -> "No world configuration found at " + worldConfigPath);
        } else {
            loadFileProperties(props, worldConfigPath);
        }
        validateProperties(props);
        return new ModConfig(props);
    }

    public static void writeDefaultConfigFile() throws IOException {
        if (!MOD_CONFIG_PATH.toFile().exists()) {
            FileUtils.writeResourceToFile(DEFAULT_MOD_CONFIG_RESOURCE, MOD_CONFIG_PATH);
        }
    }

    private static void loadFileProperties(final Properties out, final Path propertiesPath) throws IOException {
        try (InputStream in = new FileInputStream(propertiesPath.toFile())) {
            out.load(in);
        }
    }

    private static void loadDefaultProperties(final Properties properties) throws IOException {
        try (final InputStream in = ModConfig.class.getResourceAsStream(DEFAULT_PROPERTIES_RESOURCE)) {
            if (in == null) {
                throw new FileNotFoundException("Unable to load resource " + DEFAULT_PROPERTIES_RESOURCE);
            }
            properties.load(in);
        }
    }

    /**
     * Sanity check that every property is configured
     */
    private static void validateProperties(final Properties properties) throws IOException {
        for (final Key key : Key.values()) {
            if (properties.getProperty(key.propertyName) == null) {
                throw new IOException(key.propertyName + " not configured");
            }
        }
    }
}

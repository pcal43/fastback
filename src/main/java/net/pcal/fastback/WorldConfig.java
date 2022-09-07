package net.pcal.fastback;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;

record WorldConfig(
        boolean isBackupEnabled,
        boolean isShutdownBackupEnabled,
        boolean isRemoteBackupEnabled,
        String getRemotePushUri) {

    String getRemoteName() {
        return REMOTE_NAME;
    }

    String getUuidCheckPrefix() {
        return "FIXME";
    }

    boolean isUuidCheckEnabled() {
        return true;
    }

    boolean isTempBranchCleanupEnabled() {
        return true;
    }

    boolean isFileRemoteTempBranchCleanupEnabled() {
        return true;
    }

    String getTempBranchNameFormat() {
        return "temp/%s";
    }

    String getSnapshotPrefix() {
        return "snapshot";
    }

    String getDateFormat() {
        return "yyyy-MM-dd_HH-mm-ss";
    }

    private static final String REMOTE_NAME = "origin";
    private static final String CONFIG_SECTION = "fastback";
    private static final String CONFIG_BACKUP_ENABLED = "backup-enabled";
    private static final String CONFIG_SHUTDOWN_BACKUP_ENABLED = "shutdown-backup-enabled";
    private static final String CONFIG_REMOTE_BACKUP_ENABLED = "remote-backup-enabled";

    static WorldConfig load(Path worldSaveDir) throws IOException {
        try(final Git git = Git.open(worldSaveDir.toFile())) {
            final StoredConfig config = git.getRepository().getConfig();
            return new WorldConfig(
                    config.getBoolean(CONFIG_SECTION, null, CONFIG_BACKUP_ENABLED, false),
                    config.getBoolean(CONFIG_SECTION, null, CONFIG_SHUTDOWN_BACKUP_ENABLED, false),
                    config.getBoolean(CONFIG_SECTION, null, CONFIG_REMOTE_BACKUP_ENABLED, false),
                    config.getString("remote", REMOTE_NAME, "url")
            );
        }
    }

    static void setRemoteUrl(Path worldSaveDir, String url) throws IOException {
        try(final Git git = Git.open(worldSaveDir.toFile())) {
            git.getRepository().getConfig().setString("remote" , REMOTE_NAME, "url", url);
        }
    }

    static void setBackupEnabled(Path worldSaveDir, boolean value) throws IOException {
        setBoolean(worldSaveDir, value, CONFIG_BACKUP_ENABLED);
    }

    static void setShutdownBackupEnabled(Path worldSaveDir, boolean value) throws IOException {
        setBoolean(worldSaveDir, value, CONFIG_SHUTDOWN_BACKUP_ENABLED);
    }

    static void setRemoteBackupEnabled(Path worldSaveDir, boolean value) throws IOException {
        setBoolean(worldSaveDir, value, CONFIG_REMOTE_BACKUP_ENABLED);
    }

    private static void setBoolean(Path worldSaveDir, boolean value, String configKey) throws IOException {
        try(final Git git = Git.open(worldSaveDir.toFile())) {
            git.getRepository().getConfig().setBoolean(CONFIG_SECTION, null, configKey, value);
        }
    }
}



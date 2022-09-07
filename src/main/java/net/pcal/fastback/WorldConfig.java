package net.pcal.fastback;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;

public record WorldConfig(
        boolean isBackupEnabled,
        boolean isShutdownBackupEnabled,
        boolean isRemoteBackupEnabled,
        String getRemotePushUri) {

    public String getRemoteName() {
        return REMOTE_NAME;
    }

    public String getUuidCheckPrefix() {
        return "FIXME";
    }

    public boolean isUuidCheckEnabled() {
        return true;
    }

    public boolean isTempBranchCleanupEnabled() {
        return true;
    }

    public boolean isFileRemoteTempBranchCleanupEnabled() {
        return true;
    }

    public String getTempBranchNameFormat() {
        return "temp/%s";
    }

    public String getSnapshotPrefix() {
        return "snapshot";
    }

    public String getDateFormat() {
        return "yyyy-MM-dd_HH-mm-ss";
    }

    public boolean isSmartPushEnabled() {
        return true;
    }

    public String getLatestBranchName() {
        return "latest";
    }

    private static final String REMOTE_NAME = "origin";
    private static final String CONFIG_SECTION = "fastback";
    private static final String CONFIG_BACKUP_ENABLED = "backup-enabled";
    private static final String CONFIG_SHUTDOWN_BACKUP_ENABLED = "shutdown-backup-enabled";
    private static final String CONFIG_REMOTE_BACKUP_ENABLED = "remote-backup-enabled";

    public static WorldConfig load(Config gitConfig) throws IOException {
        return new WorldConfig(
                gitConfig.getBoolean(CONFIG_SECTION, null, CONFIG_BACKUP_ENABLED, false),
                gitConfig.getBoolean(CONFIG_SECTION, null, CONFIG_SHUTDOWN_BACKUP_ENABLED, false),
                gitConfig.getBoolean(CONFIG_SECTION, null, CONFIG_REMOTE_BACKUP_ENABLED, false),
                gitConfig.getString("remote", REMOTE_NAME, "url")
        );
    }

    // REMEMBER TO CALL config.save() YOURSELF!!

    public static void setRemoteUrl(Config gitConfig, String url) {
        gitConfig.setString("remote" , REMOTE_NAME, "url", url);
    }

    public static void setBackupEnabled(Config gitConfig, boolean value) {
        gitConfig.setBoolean(CONFIG_SECTION, null, CONFIG_BACKUP_ENABLED, value);
    }

    public static void setShutdownBackupEnabled(Config gitConfig, boolean value) {
        gitConfig.setBoolean(CONFIG_SECTION, null, CONFIG_SHUTDOWN_BACKUP_ENABLED, value);
    }

    public static void setRemoteBackupEnabled(Config gitConfig, boolean value) {
        gitConfig.setBoolean(CONFIG_SECTION, null, CONFIG_REMOTE_BACKUP_ENABLED, value);
    }

}



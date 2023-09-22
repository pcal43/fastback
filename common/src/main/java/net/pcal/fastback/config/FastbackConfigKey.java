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

/**
 * .gitconfig settings that fastback cares about.
 *
 * @author pcal
 */
public enum FastbackConfigKey implements GitConfigKey {

    AUTOBACK_ACTION("autoback-action", null),
    AUTOBACK_WAIT_MINUTES("autoback-wait", 0),
    BROADCAST_ENABLED("broadcast-enabled", true),
    BROADCAST_MESSAGE("broadcast-message", null),
    IS_BACKUP_ENABLED("backup-enabled", true),
    IS_BRANCH_CLEANUP_ENABLED(true),
    IS_FILE_REMOTE_BARE(true),
    IS_LOCK_CLEANUP_ENABLED("lock-cleanup-enabled", true),
    IS_NATIVE_GIT_ENABLED("native-git-enabled", false),
    IS_MODS_BACKUP_ENABLED("mods-backup-enabled", false),
    IS_REFLOG_DELETION_ENABLED(true),
    IS_REMOTE_TEMP_BRANCH_CLEANUP_ENABLED(true),
    IS_SMART_PUSH_ENABLED("smart-push-enabled", false),
    IS_TEMP_BRANCH_CLEANUP_ENABLED(true),
    IS_TRACKING_BRANCH_CLEANUP_ENABLED(true),
    IS_UUID_CHECK_ENABLED(true),
    LOCAL_RETENTION_POLICY("retention-policy", null),
    REMOTE_NAME("remote-name", "origin"),
    REMOTE_RETENTION_POLICY("remote-retention-policy", null),
    RESTORE_DIRECTORY("restore-directory", null),
    SHUTDOWN_ACTION("shutdown-action", "local"),
    UPDATE_GITATTRIBUTES_ENABLED("update-gitattributes-enabled", true),
    UPDATE_GITIGNORE_ENABLED("update-gitignore-enabled", true);

    private final String settingName;
    private final Boolean booleanDefault;
    private final String stringDefault;
    private final Integer intDefault;

    FastbackConfigKey(boolean booleanDefaultValue) {
        this(null, booleanDefaultValue, null, null);
    }

    FastbackConfigKey(final String settingName, boolean booleanDefaultValue) {
        this(settingName, booleanDefaultValue, null, null);
    }

    FastbackConfigKey(final String settingName, String stringDefaultValue) {
        this(settingName, null, stringDefaultValue, null);
    }

    FastbackConfigKey(final String settingName, int intDefault) {
        this(settingName, null, null, intDefault);
    }

    FastbackConfigKey(final String settingName, final Boolean booleanDefault, String stringDefault, Integer intDefault) {
        this.settingName = settingName; //requireNonNull(settingName);
        this.booleanDefault = booleanDefault;
        this.stringDefault = stringDefault;
        this.intDefault = intDefault;
    }


    @Override
    public String getSectionName() {
        return "fastback";
    }

    @Override
    public String getSubSectionName() {
        return null;
    }

    @Override
    public String getSettingName() {
        return this.settingName;
    }

    @Override
    public boolean getBooleanDefault() {
        if (this.booleanDefault == null) throw new IllegalStateException(this + " is not a boolean");
        return this.booleanDefault;
    }

    @Override
    public String getStringDefault() {
        return this.stringDefault;
    }

    @Override
    public int getIntDefault() {
        if (this.intDefault == null) throw new IllegalStateException(this + " is not an int");
        return this.intDefault;
    }
}

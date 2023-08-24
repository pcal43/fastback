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

    IS_BACKUP_ENABLED("backup-enabled", false),

    IS_NATIVE_GIT_ENABLED("native-git-enabled", false),

    IS_LOCK_CLEANUP_ENABLED("lock-cleanup-enabled", false),

    AUTOBACK_ACTION("autoback-action", null),

    SHUTDOWN_ACTION("shutdown-action", null),

    AUTOBACK_WAIT_MINUTES("autoback-wait", 0),

    LOCAL_RETENTION_POLICY("retention-policy", null),

    REMOTE_RETENTION_POLICY("remote-retention-policy", null),

    UPDATE_GITIGNORE_ENABLED("update-gitignore-enabled", true),

    UPDATE_GITATTRIBUTES_ENABLED("update-gitattributes-enabled", true),

    REMOTE_NAME("remote-name", "origin"),

    BROADCAST_NOTICE_MESSAGE("broadcast-notice-message", null),

    BROADCAST_NOTICE_ENABLED("broadcast-notice-enabled", true),

    IS_UUID_CHECK_ENABLED(true),

    IS_TEMP_BRANCH_CLEANUP_ENABLED(true),

    IS_TRACKING_BRANCH_CLEANUP_ENABLED(true),

    IS_REMOTE_TEMP_BRANCH_CLEANUP_ENABLED(true),

    IS_SMART_PUSH_ENABLED(true),

    IS_GIT_HUD_OUTPUT_ENABLED(true),

    IS_POST_RESTORE_CLEANUP_ENABLED(true),

    IS_REFLOG_DELETION_ENABLED(true),

    IS_BRANCH_CLEANUP_ENABLED(true),

    IS_EXPERIMENTAL_COMMANDS_ENABLED(true),

    IS_FILE_REMOTE_BARE(true);


    private static final String NULL_STRING = "";

    private final String  settingName;
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
        if (this.booleanDefault == null) throw new IllegalStateException();
        return this.booleanDefault;
    }

    @Override
    public String getStringDefault() {
        if (this.stringDefault == null) throw new IllegalStateException();
        if (this.stringDefault == NULL_STRING) return null;
        return this.stringDefault;
    }

    @Override
    public int getIntDefault() {
        if (this.intDefault == null) throw new IllegalStateException();
        return this.intDefault;
    }
}

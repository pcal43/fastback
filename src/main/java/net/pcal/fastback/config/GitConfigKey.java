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

import static java.util.Objects.requireNonNull;

/**
 * .gitconfig settings that fastback cares about.
 *
 * @author pcal
 */
public enum GitConfigKey {

    IS_BACKUP_ENABLED("backup-enabled", false),

    IS_NATIVE_GIT_ENABLED("native-git-enabled", false),

    AUTOBACK_ACTION("autoback-action", null),

    SHUTDOWN_ACTION("shutdown-action", null),

    AUTOBACK_WAIT_MINUTES("autoback-wait", 0),

    LOCAL_RETENTION_POLICY("retention-policy", null),

    REMOTE_RETENTION_POLICY("remote-retention-policy", null),

    UPDATE_GITIGNORE_ENABLED("update-gitignore-enabled", true),

    UPDATE_GITATTRIBUTES_ENABLED("update-gitattributes-enabled", true),

    REMOTE_PUSH_URL("remote", "origin", "url", null),

    REMOTE_NAME("origin"),

    /**
     * We disable commit signing on git init.  https://github.com/pcal43/fastback/issues/165
     */
    COMMIT_SIGNING_ENABLED("commit", null, "gpgsign", null),

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

    private final String sectionName, subSectionName, settingName;
    private final Boolean booleanDefault;
    private final String stringDefault;
    private final Integer intDefault;

    GitConfigKey(final String settingName, boolean booleanDefaultValue) {
        this("fastback", null, settingName, booleanDefaultValue);
    }

    GitConfigKey(final String settingName, String stringDefaultValue) {
        this("fastback", null, settingName, stringDefaultValue);
    }

    GitConfigKey(final String settingName, int intDefault) {
        this("fastback", null, settingName, intDefault);
    }

    GitConfigKey(final String sectionName,
                 final String subsectionName,
                 final String settingName,
                 final boolean booleanDefault) {
        this.sectionName = requireNonNull(sectionName);
        this.subSectionName = subsectionName;
        this.settingName = requireNonNull(settingName);
        this.booleanDefault = booleanDefault;
        this.stringDefault = null;
        this.intDefault = null;
    }


    GitConfigKey(final String sectionName,
                 final String subsectionName,
                 final String settingName,
                 final String stringDefault) {
        this.sectionName = requireNonNull(sectionName);
        this.subSectionName = subsectionName;
        this.settingName = requireNonNull(settingName);
        this.stringDefault = stringDefault == null ? NULL_STRING : requireNonNull(stringDefault);
        this.booleanDefault = null;
        this.intDefault = null;
    }

    GitConfigKey(final String sectionName,
                 final String subsectionName,
                 final String settingName,
                 final int intDefault) {
        this.sectionName = requireNonNull(sectionName);
        this.subSectionName = subsectionName;
        this.settingName = requireNonNull(settingName);
        this.intDefault = intDefault;
        this.booleanDefault = null;
        this.stringDefault = null;
    }

    /**
     * For currently-immutable settings that might become configurable someday.
     */
    GitConfigKey(final boolean booleanDefault) {
        this.sectionName = null;
        this.subSectionName = null;
        this.settingName = null;
        this.booleanDefault = booleanDefault;
        this.intDefault = null;
        this.stringDefault = null;
    }

    GitConfigKey(final String stringDefault) {
        this.sectionName = null;
        this.subSectionName = null;
        this.settingName = null;
        this.stringDefault = stringDefault;
        this.booleanDefault = null;
        this.intDefault = null;
    }

    String getSectionName() {
        return this.sectionName;
    }

    String getSubSectionName() {
        return this.subSectionName;
    }

    String getSettingName() {
        return this.settingName;
    }

    boolean getBooleanDefault() {
        if (this.booleanDefault == null) throw new IllegalStateException();
        return this.booleanDefault;
    }

    String getStringDefault() {
        if (this.stringDefault == null) throw new IllegalStateException();
        if (this.stringDefault == NULL_STRING) return null;
        return this.stringDefault;
    }

    int getIntDefault() {
        if (this.intDefault == null) throw new IllegalStateException();
        return this.intDefault;
    }

    boolean isConfigurable() {
        return this.settingName != null;
    }
}

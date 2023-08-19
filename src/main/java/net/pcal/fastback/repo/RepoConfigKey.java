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

package net.pcal.fastback.repo;

import static java.util.Objects.requireNonNull;

public enum RepoConfigKey {

    IS_BACKUP_ENABLED("backup-enabled", false),

    AUTOBACK_ACTION("autoback-action", null),

    SHUTDOWN_ACTION("shutdown-action", null),

    AUTOBACK_WAIT_MINUTES("autoback-wait", 0),

    LOCAL_RETENTION_POLICY("retention-policy", null),

    REMOTE_RETENTION_POLICY("remote-retention-policy", null),

    UPDATE_GITIGNORE_ENABLED("update-gitignore-enabled", true),

    UPDATE_GITATTRIBUTES_ENABLED("update-gitattributes-enabled", true),

    REMOTE_PUSH_URL("remote", "origin", "url", null),

    IS_UUID_CHECK_ENABLED(true),

    IS_TEMP_BRANCH_CLEANUP_ENABLED(true),

    IS_TRACKING_BRANCH_CLEANUP_ENABLED(true),

    IS_REMOTE_TEMP_BRANCH_CLEANUP_ENABLED(true),

    IS_SMART_PUSH_ENABLED(true),

    IS_POST_RESTORE_CLEANUP_ENABLED(true);

    private final String sectionName, subSectionName, settingName;
    private final Boolean booleanDefault;
    private final String stringDefault;
    private final Integer intDefault;

    RepoConfigKey(final String settingName, boolean booleanDefaultValue) {
        this("fastback", null, settingName, booleanDefaultValue);
    }

    RepoConfigKey(final String settingName, String stringDefaultValue) {
        this("fastback", null, settingName, stringDefaultValue);
    }

    RepoConfigKey(final String settingName, int intDefault) {
        this("fastback", null, settingName, intDefault);
    }

    RepoConfigKey(final String sectionName,
                  final String subsectionName,
                  final String settingName,
                  final boolean booleanDefault) {
        this.sectionName = requireNonNull(sectionName);
        this.subSectionName = requireNonNull(subsectionName);
        this.settingName = requireNonNull(settingName);
        this.booleanDefault = booleanDefault;
        this.stringDefault = null;
        this.intDefault = null;
    }


    RepoConfigKey(final String sectionName,
                  final String subsectionName,
                  final String settingName,
                  final String stringDefault) {
        this.sectionName = requireNonNull(sectionName);
        this.subSectionName = requireNonNull(subsectionName);
        this.settingName = requireNonNull(settingName);
        this.stringDefault = stringDefault;
        this.booleanDefault = null;
        this.intDefault = null;
    }

    RepoConfigKey(final String sectionName,
                  final String subsectionName,
                  final String settingName,
                  final int intDefault) {
        this.sectionName = requireNonNull(sectionName);
        this.subSectionName = requireNonNull(subsectionName);
        this.settingName = requireNonNull(settingName);
        this.intDefault = intDefault;
        this.booleanDefault = null;
        this.stringDefault = null;
    }

    /**
     * For currently-immutable settings that might become configurable someday.
     */
    RepoConfigKey(final boolean booleanDefault) {
        this.sectionName = null;
        this.subSectionName = null;
        this.settingName = null;
        this.booleanDefault = booleanDefault;
        this.intDefault = null;
        this.stringDefault = null;
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
        return this.stringDefault;
    }

    int getIntDefault() {
        return this.intDefault;
    }
}

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
public enum OtherConfigKey implements GitConfigKey {

    REMOTE_PUSH_URL("remote", "origin", "url"),

    /**
     * We disable commit signing on git init.  https://github.com/pcal43/fastback/issues/165
     */
    COMMIT_SIGNING_ENABLED("commit", null, "gpgsign");

    private final String sectionName, subSectionName, settingName;

    OtherConfigKey(final String sectionName,
                   final String subsectionName,
                   final String settingName) {
        this.sectionName = requireNonNull(sectionName);
        this.subSectionName = subsectionName;
        this.settingName = requireNonNull(settingName);
    }

    @Override
    public String getSectionName() {
        return this.sectionName;
    }

    @Override
    public String getSubSectionName() {
        return this.subSectionName;
    }

    @Override
    public String getSettingName() {
        return this.settingName;
    }

    @Override
    public boolean getBooleanDefault() {
        return false;
    }

    @Override
    public String getStringDefault() {
        return null;
    }

    @Override
    public int getIntDefault() {
        return 0;
    }
}

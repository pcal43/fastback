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

import net.minecraft.network.chat.Component;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.UserLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.pcal.fastback.config.FastbackConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.*;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.*;
import static net.pcal.fastback.logging.UserMessage.styledRaw;
import static net.pcal.fastback.utils.ProcessUtils.doExec;

public class EnvironmentUtils {

    public static String getGitVersion() {
        return execForVersion(new String[]{"git", "--version"});
    }

    public static String getGitLfsVersion() {
        return execForVersion(new String[]{"git", "lfs", "--version"});
    }

    /**
     * @return true if native git is installed correctly, or if this is a legacy jgit-based backup.
     */
    public static boolean isNativeOk(final GitConfig conf, final UserLogger ulog, final boolean verbose) {
        return isNativeOk(conf.getBoolean(IS_NATIVE_GIT_ENABLED), ulog, verbose);
    }

    /**
     * @return true if native git is installed correctly, or if this is a legacy jgit-based backup.
     */
    public static boolean isNativeOk(boolean isNativeGitEnabled, UserLogger ulog, boolean verbose) {
        if (isNativeGitEnabled) { // default is true; false is undocumented and deprecated
            final Component notInstalled = Component.translatable("fastback.values.not-installed");
            final String gitVersion = getGitVersion();
            final String gitLfsVersion = getGitLfsVersion();
            if (verbose) {
                ulog.message(localized("fastback.chat.native-info",
                        gitVersion != null ? gitVersion : notInstalled,
                        gitLfsVersion != null ? gitLfsVersion : notInstalled));
            }
            if (gitVersion == null) {
                ulog.message(styledLocalized("fastback.chat.native-git-not-installed", ERROR, System.getenv("PATH")));
                return false;
            }
            if (gitLfsVersion == null) {
                ulog.message(styledLocalized("fastback.chat.native-lfs-not-installed", ERROR, System.getenv("PATH")));
                return false;
            }
        } else {
            if (verbose) ulog.message(styledLocalized("fastback.chat.native-disabled", WARNING));
        }
        return true;
    }

    private static String execForVersion(String[] cmd) {
        final List<String> stdout = new ArrayList<>();
        final int exit;
        try {
            exit = doExec(cmd, Collections.emptyMap(), stdout::add, line -> {
            });
        } catch (ProcessException e) {
            syslog().debug("Could not run " + String.join(" ", cmd), e);
            return null;
        }
        return exit == 0 ? stdout.get(0) : null;
    }
}

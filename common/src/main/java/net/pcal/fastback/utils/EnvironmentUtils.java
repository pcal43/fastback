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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.utils.ProcessUtils.doExec;

public class EnvironmentUtils {

    public static boolean isNativeGitInstalled() {
        return getGitVersion() != null && getGitLfsVersion() != null;
    }

    public static String getGitVersion() {
        return execForVersion(new String[]{"git", "--version"});
    }

    public static String getGitLfsVersion() {
        return execForVersion(new String[]{"git-lfs", "--version"});
    }

    private static String execForVersion(String[] cmd) {
        final List<String> stdout = new ArrayList<>();
        final int exit;
        try {
            exit = doExec(cmd, Collections.emptyMap(), stdout::add, line -> {
            });
        } catch (IOException | InterruptedException e) {
            syslog().debug("Could not run " + String.join(" ", cmd), e);
            return null;
        }
        return exit == 0 ? stdout.get(0) : null;
    }


}

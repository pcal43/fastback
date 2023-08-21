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

import net.pcal.fastback.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.pcal.fastback.utils.ExecUtils.doExec;

public class NativeGitUtils {

    public static boolean isNativeGitInstalled(final Logger log) {
        return getGitVersion(log) != null && getGitLfsVersion(log) != null;
    }

    public static String getGitVersion(final Logger log) {
        return execForVersion(new String[] {"git", "-v"}, log);
    }

    public static String getGitLfsVersion(final Logger log) {
        return execForVersion(new String[] {"git-lfs", "-v"}, log);
    }

    private static String execForVersion(String[] cmd, Logger log) {
        final List<String> stdout = new ArrayList<>();
        final int exit;
        try {
            exit = doExec(cmd, Collections.emptyMap(), stdout::add, line -> {}, log);
        } catch (IOException | InterruptedException e) {
            log.debug("Could not run "+ String.join(" ", cmd), e);
            return null;
        }
        return exit == 0 ? stdout.get(0) : null;
    }

}

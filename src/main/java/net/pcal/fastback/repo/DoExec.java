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

import net.pcal.fastback.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 *
 *
 */
class DoExec {

    static void doExec(String[] args, Logger log) throws IOException {
        log.info(String.join(" ", args));
        Map<String,String> env = new HashMap<>();
        env.putAll(System.getenv());
        env.put("GIT_TRACE", "1");
        env.put("GIT_CURL_VERBOSE", "1");

        pretty sure the problem is you're not draining stderr

        List<String> envlist = new ArrayList<>();
        for(Map.Entry<String, String> entry : env.entrySet()) {
            envlist.add(entry.getKey()+"="+entry.getValue());
        }
        String[] enva = envlist.toArray(new String[0]);
        final Process p = Runtime.getRuntime().exec(args, enva);
        String stdout = readString(p.getInputStream());
        String stderr = readString(p.getErrorStream());
        log.info(stdout);
        log.info(stderr);
        log.info("exitCode = "+p.exitValue());
    }

    private static String readString(InputStream in) {
        return new BufferedReader(new InputStreamReader(in))
                .lines().collect(Collectors.joining("\n"));
    }


}

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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.SystemLogger.syslog;


/**
 *
 */
public class ProcessUtils {

    public static int doExec(String[] args, final Map<String, String> envOriginal, Consumer<String> stdoutSink, Consumer<String> stderrSink) throws ProcessException {
        return doExec(args, envOriginal, stdoutSink, stderrSink, true);
    }

    public static int doExec(final String[] args, final Map<String, String> envOriginal, final Consumer<String> stdoutSink, final Consumer<String> stderrSink, boolean throwOnNonZero) throws ProcessException {
        syslog().debug("Executing " + String.join(" ", args));
        final Map<String, String> env = new HashMap<>(envOriginal);
        env.putAll(System.getenv());
        // output a few values that are important for debugging; don't indiscriminately dump everything or someone's going
        // to end up uploading a bunch of passwords into pastebin.
        syslog().debug("PATH: " + env.get("PATH"));
        syslog().debug("USER: " + env.get("USER"));
        syslog().debug("HOME: " + env.get("HOME"));
        final List<String> errorBuffer = new ArrayList<>();
        final Consumer<String> stdout = line->{
            syslog().debug("[STDOUT] " + line);
            stdoutSink.accept(line);
            errorBuffer.add("[STDOUT] " + line);
        };
        final Consumer<String> stderr = line->{
            syslog().debug("[STDERR] " + line);
            stderrSink.accept(line);
            errorBuffer.add("[STDERR] " + line);
        };
        final List<String> envlist = new ArrayList<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            envlist.add(entry.getKey() + "=" + entry.getValue());
        }
        final String[] enva = envlist.toArray(new String[0]);
        final int exit;
        try {
            final Process p = Runtime.getRuntime().exec(args, enva);
            exit = drainAndWait(p, new LineWriter(stdout), new LineWriter(stderr));
        } catch (IOException | InterruptedException e) {
            throw new ProcessException(args, 0, errorBuffer, e);
        }
        if (exit != 0) {
            throw new ProcessException(args, exit, errorBuffer);
        }
        return exit;
    }

    private static class LineWriter extends Writer {

        private final Consumer<String> sink;
        private final StringBuilder buffer = new StringBuilder();

        private LineWriter(final Consumer<String> sink) {
            this.sink = requireNonNull(sink);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            buffer.append(cbuf, off, len);
            outputLines();
        }

        private void outputLines() {
            int lineStart = 0, lineEnd;
            while ((lineEnd = findLineEnd(buffer, lineStart)) != -1) {
                final String line = buffer.substring(lineStart, lineEnd).trim();
                if (line.length() > 0) this.sink.accept(line);
                lineStart = lineEnd + 1;
            }
            if (lineStart != 0) {
                buffer.delete(0, lineStart);
            }
        }

        private static int findLineEnd(StringBuilder buffer, int lineStart) {
            int newLine = buffer.indexOf("\n", lineStart);
            int carriage = buffer.indexOf("\r", lineStart);
            if (newLine == -1) return carriage;
            if (carriage == -1) return newLine;
            return Math.min(newLine, carriage);
        }

        @Override
        public void flush() throws IOException {
            outputLines();
            if (buffer.length() > 0) {
                this.sink.accept(buffer.toString());
            }
        }

        @Override
        public void close() throws IOException {
        }
    }

    private static int drainAndWait(Process process, Writer stdoutSink, Writer stderrSink) throws IOException, InterruptedException {

        Reader stdoutReader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
        Reader stderrReader = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8);

        char[] buffer = new char[1024];

        while (true) {
            boolean readAny = false;
            //
            // process stdin
            //
            if (stdoutReader != null && stdoutReader.ready()) {
                int read = stdoutReader.read(buffer, 0, buffer.length);
                if (read < 0) {
                    stdoutReader = null;
                } else if (read > 0) {
                    readAny = true;
                    stdoutSink.write(buffer, 0, read);
                }
            }
            //
            // process stdout
            //
            if (stderrReader != null && stderrReader.ready()) {
                int read = stderrReader.read(buffer, 0, buffer.length);
                if (read < 0) {
                    stderrReader = null;
                } else if (read > 0) {
                    readAny = true;
                    stderrSink.write(buffer, 0, read);
                }
            }

            if (readAny) {
                continue;
            } else if (!process.isAlive()) {
                return process.exitValue();
            } else {
                try {
                    Thread.sleep(10); // FIXME add timeout?
                } catch (InterruptedException ie) {
                    process.destroy();
                    throw ie;
                }
            }
        }
    }
}

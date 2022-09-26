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

package net.pcal.fastback.commands;

import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.tasks.CommitAndPushTask;
import net.pcal.fastback.tasks.CommitTask;
import org.eclipse.jgit.api.Git;

import static java.util.Objects.requireNonNull;

/**
 * Encapsulates an action that can be performed in response to events such as shutdown or autosaving.
 *
 * @author pcal
 * @since 0.2.0
 */
public enum SchedulableAction {

    NONE("none") {
        @Override
        public Runnable getRunnable(Git git, ModContext ctx, Logger log) {
            return () -> {
            };
        }
    },

    LOCAL("local") {
        @Override
        public Runnable getRunnable(Git git, ModContext ctx, Logger log) {
            return new CommitTask(git, ctx, log);
        }
    },

    FULL("full") {
        @Override
        public Runnable getRunnable(Git git, ModContext ctx, Logger log) {
            return new CommitAndPushTask(git, ctx, log);
        }
    };

    public static SchedulableAction getForConfigKey(String configKey) {
        for (SchedulableAction action : SchedulableAction.values()) {
            if (action.configKey.equals(configKey)) return action;
        }
        return null;
    }

    private final String configKey;

    SchedulableAction(String configKey) {
        this.configKey = requireNonNull(configKey);
    }

    public String getConfigKey() {
        return this.configKey;
    }

    public abstract Runnable getRunnable(Git git, ModContext ctx, Logger log);
}


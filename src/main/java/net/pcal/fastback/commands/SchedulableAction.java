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

import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.config.GitConfigKey;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.SnapshotId;

import java.util.Collection;
import java.util.concurrent.Callable;

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
        public Callable<Void> getTask(final Repo repo) {
            return () -> null;
        }
    },

    LOCAL("local") {
        @Override
        public Callable<Void> getTask(final Repo repo) {
            return ()->{ repo.doCommitSnapshot(); return null; };
        }
    },

    FULL("full") {
        @Override
        public Callable<Void> getTask(final Repo repo) {
            return ()->{ repo.doCommitAndPush(); return null; };
        }
    },

    FULL_GC("full-gc") {
        @Override
        public Callable<Void> getTask(final Repo repo) {
            return ()->{
                repo.doCommitAndPush();
                final Collection<SnapshotId> pruned = repo.doLocalPrune();
                if (pruned.size() > 0) {
                    repo.doGc();
                }
                return null;
            };
        }
    };

    public static final SchedulableAction DEFAULT_SHUTDOWN_ACTION = FULL;

    public static SchedulableAction forConfigValue(final GitConfig c, final GitConfigKey key) {
        String configValue = c.getString(key);
        if (configValue == null) return null;
        return forConfigValue(configValue);
    }
    public static SchedulableAction forConfigValue(String configValue) {
        if (configValue == null) return null;
        for (SchedulableAction action : SchedulableAction.values()) {
            if (action.configValue.equals(configValue)) return action;
        }
        return null;
    }

    private final String configValue;

    SchedulableAction(String configValue) {
        this.configValue = requireNonNull(configValue);
    }

    public String getConfigValue() {
        return this.configValue;
    }

    public String getArgumentName() {
        return this.configValue;
    }

    public abstract Callable<?> getTask(Repo repo);
}


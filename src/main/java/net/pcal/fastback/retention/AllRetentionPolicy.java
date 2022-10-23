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

package net.pcal.fastback.retention;

import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Message;
import net.pcal.fastback.utils.SnapshotId;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import static net.pcal.fastback.logging.Message.localized;

/**
 * Policy to retain all snapshots.
 *
 * @author pcal
 * @since 0.2.0
 */
enum AllRetentionPolicy implements RetentionPolicy {

    INSTANCE;

    private static final String L10N_KEY = "fastback.retain.all.description";

    @Override
    public Message getDescription() {
        return localized(L10N_KEY);
    }

    @Override
    public Collection<SnapshotId> getSnapshotsToPrune(NavigableSet<SnapshotId> fromSnapshots) {
        return Collections.emptySet();
    }

    enum Type implements RetentionPolicyType {

        INSTANCE;

        @Override
        public String getName() {
            return "all";
        }

        @Override
        public List<Parameter> getParameters() {
            return Collections.emptyList();
        }

        @Override
        public RetentionPolicy createPolicy(final ModContext ctx, final Map<String, String> config) {
            return AllRetentionPolicy.INSTANCE;
        }

        @Override
        public Message getDescription() {
            return localized(L10N_KEY);
        }
    }
}

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

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.repo.SnapshotId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.pcal.fastback.logging.SystemLogger.syslog;

/**
 * Retention policy that keeps only the n most-recent snapshots.
 *
 * @author pcal
 * @since 0.2.0
 */
class FixedCountRetentionPolicy implements RetentionPolicy {

    private static final int COUNT_DEFAULT = 10;
    private static final String POLICY_NAME = "fixed";
    private static final String L10N_KEY = "fastback.retain.fixed.description";
    private static final String COUNT_PARAM = "count";
    private final int count;

    public static FixedCountRetentionPolicy create(Map<String, String> config) {
        int count = COUNT_DEFAULT;
        if (config != null && config.containsKey(COUNT_PARAM)) {
            try {
                count = Integer.parseInt(config.get(COUNT_PARAM));
            } catch (NumberFormatException nfe) {
                syslog().debug("Ignoring invalided fixed count " + config.get(COUNT_PARAM), nfe);
            }
        }
        return new FixedCountRetentionPolicy(count);
    }

    private FixedCountRetentionPolicy(int count) {
        this.count = count;
    }

    @Override
    public UserMessage getDescription() {
        return UserMessage.localized(L10N_KEY, this.count);
    }

    @Override
    public Collection<SnapshotId> getSnapshotsToPrune(Set<SnapshotId> fromSnapshots) {
        final List<SnapshotId> sorted = new ArrayList<>(fromSnapshots);
        sorted.sort(Collections.reverseOrder());
        if (sorted.size() > count) {
            return sorted.subList(count - 1, sorted.size() - 1);
        } else {
            return Collections.emptySet();
        }
    }

    enum Type implements RetentionPolicyType {

        INSTANCE;

        @Override
        public String getName() {
            return POLICY_NAME;
        }

        @Override
        public List<Parameter<?>> getParameters() {
            return List.of(new Parameter<>(COUNT_PARAM, IntegerArgumentType.integer(1), Integer.class));
        }

        @Override
        public RetentionPolicy createPolicy(final Map<String, String> config) {
            return create(config);
        }

        @Override
        public UserMessage getDescription() {
            return UserMessage.localized(L10N_KEY, "<" + COUNT_PARAM + ">");
        }
    }
}

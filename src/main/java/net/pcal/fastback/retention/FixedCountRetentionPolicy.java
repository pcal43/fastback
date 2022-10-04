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
import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Message;
import net.pcal.fastback.utils.SnapshotId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static net.pcal.fastback.logging.Message.localized;

/**
 * Retention policy that keeps only the n most-recent snapshots.
 *
 * @author pcal
 * @since 0.2.0
 */
class FixedCountRetentionPolicy implements RetentionPolicy {

    private static final int COUNT_DEFAULT = 10;
    private static final String COUNT_PARAM = "count";
    private static final String L10N_KEY = "fastback.retain.count.description";
    private final ModContext ctx;
    private final int count;

    public static FixedCountRetentionPolicy create(Map<String, String> config, ModContext ctx) {
        int count = COUNT_DEFAULT;
        if (config != null && config.containsKey(COUNT_PARAM)) {
            try {
                count = Integer.parseInt(config.get(COUNT_PARAM));
            } catch (NumberFormatException nfe) {
                ctx.getLogger().internalError("invalid count " + config.get(COUNT_PARAM), nfe);
            }
        }
        return new FixedCountRetentionPolicy(ctx, count);
    }

    private FixedCountRetentionPolicy(ModContext ctx, int count) {
        this.ctx = ctx;
        this.count = count;
    }

    @Override
    public Message getDescription() {
        return localized(L10N_KEY, this.count);
    }

    @Override
    public Collection<SnapshotId> getSnapshotsToPrune(Collection<SnapshotId> fromSnapshots) {
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
            return "fixed";
        }

        @Override
        public List<Parameter> getParameters() {
            return List.of(new Parameter(COUNT_PARAM, IntegerArgumentType.integer(1)));
        }

        @Override
        public RetentionPolicy createPolicy(final ModContext ctx, final Map<String, String> config) {
            return create(config, ctx);
        }

        @Override
        public Message getDescription() {
            return localized(L10N_KEY, "<" + COUNT_PARAM + ">");
        }
    }
}

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

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.function.Supplier;

import static net.pcal.fastback.logging.Message.localized;

/**
 * Policy that implements a simple 'Grandfather-Father-Son' strategy.  It retains
 * - every backup in the last 24 hours
 * - the latest daily backup for the past week
 * - the latest weekly backup for the past month
 * - the latest monthly backup for all past months
 *
 * @author pcal
 * @since 0.2.1
 */
class GFSRetentionPolicy implements RetentionPolicy {

    private static final String L10N_KEY = "fastback.retain.gfs.description";
    private final ModContext ctx;
    Supplier<LocalDate> nowSupplier = () ->
            LocalDate.now(GFSRetentionPolicy.this.ctx.getTimeZone().toZoneId());

    public GFSRetentionPolicy(final ModContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Message getDescription() {
        return localized(L10N_KEY);
    }

    @Override
    public Collection<SnapshotId> getSnapshotsToPrune(NavigableSet<SnapshotId> snapshots) {
        final List<SnapshotId> toPrune = new ArrayList<>();
        final LocalDate now = LocalDate.now(ctx.getTimeZone().toZoneId());
        final LocalDate gracePeriodStart = now.minus(Period.ofDays(2));
        final LocalDate oneWeekAgo = now.minus(Period.ofDays(7));
        final LocalDate oneMonthAgo = now.minus(Period.ofDays(30));
        Integer currentDay = null, currentWeek = null, currentMonth = null;
        for (final SnapshotId sid : snapshots.descendingSet()) {
            final LocalDate snapshotDate = sid.snapshotDate().toInstant().atZone(ctx.getTimeZone().toZoneId()).toLocalDate();
            if (snapshotDate.isAfter(gracePeriodStart)) {
                ctx.getLogger().debug("Will retain " + sid + " because still in the grace period");
            } else if (snapshotDate.isAfter(oneWeekAgo)) {
                final int snapshotDay = snapshotDate.get(ChronoField.DAY_OF_MONTH);
                if (currentDay == null || currentDay != snapshotDay) {
                    currentDay = snapshotDay;
                } else {
                    toPrune.add(sid);
                }
            } else if (snapshotDate.isAfter(oneMonthAgo)) {
                final int snapshotWeek = snapshotDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                if (currentWeek == null || currentWeek != snapshotWeek) {
                    currentWeek = snapshotWeek;
                } else {
                    toPrune.add(sid);
                }
            } else {
                final int snapshotMonth = snapshotDate.get(ChronoField.MONTH_OF_YEAR);
                if (currentMonth == null || snapshotMonth != currentMonth) {
                    currentMonth = snapshotMonth;
                } else {
                    toPrune.add(sid);
                }
            }
        }
        return toPrune;
    }

    /**
     * Retention policy that keeps only the most-recent snapshot of each day.  Provides for a grace period
     * during which all snapshots are retained.
     *
     * @author pcal
     * @since 0.2.0
     */
    public enum GFSRetentionPolicyType implements RetentionPolicyType {

        INSTANCE;

        @Override
        public String getName() {
            return "gfs";
        }

        @Override
        public List<Parameter> getParameters() {
            return Collections.emptyList();
        }

        @Override
        public RetentionPolicy createPolicy(final ModContext ctx, final Map<String, String> config) {
            return new GFSRetentionPolicy(ctx);
        }

        @Override
        public Message getDescription() {
            return localized(L10N_KEY);
        }
    }
}

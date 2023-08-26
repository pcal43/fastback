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

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TimeZone;

import static net.pcal.fastback.logging.SystemLogger.syslog;

/**
 * Policy that retains only the last snapshot of each day, along with all snapshots in
 * the last n days.
 *
 * @author pcal
 * @since 0.2.0
 */
class DailyRetentionPolicy implements RetentionPolicy {

    private static final String GRACE_PERIOD_DAYS = "gracePeriodDays";
    private static final int DEFAULT_GRACE_PERIOD_DAYS = 3;
    private static final String L10N_KEY = "fastback.retain.daily.description";

    private final int gracePeriod;

    public DailyRetentionPolicy(int gracePeriod) {
        this.gracePeriod = gracePeriod;
    }

    @Override
    public UserMessage getDescription() {
        return UserMessage.localized(L10N_KEY, gracePeriod);
    }

    @Override
    public Collection<SnapshotId> getSnapshotsToPrune(NavigableSet<SnapshotId> snapshots) {
        final LocalDate today = LocalDate.now(TimeZone.getDefault().toZoneId());
        final LocalDate gracePeriodStart = today.minus(Period.ofDays(gracePeriod));
        final List<SnapshotId> toPrune = new ArrayList<>();
        LocalDate previousDate = null;
        for (final SnapshotId sid : snapshots.descendingSet()) {
            final LocalDate currentDate = sid.getDate().toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalDate();
            if (previousDate != null) {
                if (currentDate.isAfter(gracePeriodStart)) {
                    syslog().debug("Will retain " + sid + " because still in the grace period");
                    continue;
                }
                if (currentDate.equals(previousDate)) {
                    syslog().debug("Will prune " + sid + " same day as " + currentDate);
                    toPrune.add(sid);
                } else {
                    syslog().debug("Will retain " + sid + " NOT same day as " + currentDate);
                }
            }
            previousDate = currentDate;
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
    public enum DailyRetentionPolicyType implements RetentionPolicyType {

        INSTANCE;

        @Override
        public String getName() {
            return "daily";
        }

        @Override
        public List<Parameter<?>> getParameters() {
            return List.of(new Parameter<>(GRACE_PERIOD_DAYS, IntegerArgumentType.integer(0), Integer.class));
        }

        @Override
        public RetentionPolicy createPolicy(final Map<String, String> config) {
            int gracePeriodTemp = DEFAULT_GRACE_PERIOD_DAYS;
            if (config != null && config.containsKey(GRACE_PERIOD_DAYS)) {
                try {
                    gracePeriodTemp = Integer.parseInt(config.get(GRACE_PERIOD_DAYS));
                } catch (NumberFormatException nfe) {
                    syslog().debug("Ignoring invalid grace period " + config.get(GRACE_PERIOD_DAYS), nfe);
                }
            }
            final int gracePeriod = gracePeriodTemp;

            return new DailyRetentionPolicy(gracePeriod);
        }

        @Override
        public UserMessage getDescription() {
            return UserMessage.localized(L10N_KEY, "<" + GRACE_PERIOD_DAYS + ">");
        }
    }
}

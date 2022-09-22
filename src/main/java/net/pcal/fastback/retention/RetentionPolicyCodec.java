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

import com.google.common.collect.ImmutableList;
import net.pcal.fastback.ModContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public enum RetentionPolicyCodec {

    INSTANCE;

    private final List<RetentionPolicyType> POLICY_TYPES = ImmutableList.of(
            DailyRetentionPolicyType.INSTANCE
    );

    private final Map<String, RetentionPolicyType> key2enum;

    RetentionPolicyCodec() {
        key2enum = new HashMap<>();
        for(DailyRetentionPolicyType type : DailyRetentionPolicyType.values()) {
            key2enum.put(type.getConfigKey(), type);
        }
    }

    public List<RetentionPolicyType> getPolicyTypes() {
        return POLICY_TYPES;
    }

    public RetentionPolicy decodePolicy(final ModContext ctx, final String encodedPolicy) {
        final String typeConfigKey = encodedPolicy.trim(); //FIXME
        final RetentionPolicyType type = this.key2enum.get(typeConfigKey);
        if (type == null) {
            ctx.getLogger().warn("Invalid retention policy configuration: " + typeConfigKey);
        }
        final Properties decodedProperties = new Properties(); //TODO
        return type.createPolicy(ctx, decodedProperties);
    }

    public String encodePolicy(final ModContext ctx, RetentionPolicy policy) {
        final Properties decodedProperties = policy.getProperties(); //TODO
        return policy.getType().getConfigKey();
    }
}

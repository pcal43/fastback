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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.SystemLogger.syslog;

/**
 * Singleton which can encode a RetentionPolicy into a single-line string that can easily be saved in git config.
 *
 * @author pcal
 * @since 0.2.0
 */
public enum RetentionPolicyCodec {

    INSTANCE;

    public RetentionPolicy decodePolicy(final List<RetentionPolicyType> availablePolicyTypes,
                                        final String encodedPolicyOriginal) {
        requireNonNull(availablePolicyTypes);
        requireNonNull(encodedPolicyOriginal);
        final String encodedPolicy = encodedPolicyOriginal.trim();
        int firstSpace = encodedPolicy.indexOf(' ');
        final Map<String, String> config;
        final String encodedTypeName;
        if (firstSpace == -1) {
            config = null;
            encodedTypeName = encodedPolicy.trim();
        } else {
            encodedTypeName = encodedPolicy.substring(0, firstSpace).trim();
            config = decodeMap(encodedPolicy.substring(firstSpace + 1));
        }
        for (final RetentionPolicyType rtp : availablePolicyTypes) {
            if (rtp.getEncodedName().equals(encodedTypeName)) {
                return rtp.createPolicy(config);
            }
        }
        syslog().debug("Ignoring invalid retention policy " + encodedPolicy);
        return null;
    }

    public String encodePolicy(final RetentionPolicyType policyType, final Map<String, String> config) {
        return policyType.getEncodedName() + " " + encodeMap(config);
    }

    // ====================================================================
    // Package-private methods

    static String encodeMap(Map<String, String> map) {
        final StringBuilder out = new StringBuilder();
        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        boolean isFirst = true;
        for (final String key : keys) {
            if (!isValidForEncode(key)) {
                syslog().debug("Ignoring invalid key " + key);
                continue;
            }
            final String value = map.get(key);
            if (!isValidForEncode(value)) {
                syslog().debug("Ignoring invalid value " + value);
                continue;
            }
            if (!isFirst) {
                out.append(' ');
            } else {
                isFirst = false;
            }
            out.append(key);
            out.append('=');
            out.append(value);

        }
        return out.toString();
    }

    static Map<String, String> decodeMap(String encodedMap) {
        final Map<String, String> out = new HashMap<>();
        final String[] tokens = encodedMap.split(" ");
        for (final String token : tokens) {
            final String[] keyVal = token.split("=");
            if (keyVal.length != 2) {
                syslog().debug("Ignoring invalid token " + Arrays.toString(keyVal));
                continue;
            }
            out.put(keyVal[0].trim(), keyVal[1].trim());
        }
        return out;
    }

    private static boolean isValidForEncode(String keyOrVal) {
        return !(keyOrVal.contains("=") || keyOrVal.contains(" "));
    }
}

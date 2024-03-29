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

import net.pcal.fastback.logging.Log4jLogger;
import net.pcal.fastback.logging.SystemLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.repo.SnapshotId;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RetentionPolicyCodecTest {

    @BeforeAll
    public static void setup() {
        SystemLogger.Singleton.register(new Log4jLogger(LogManager.getLogger("mocklogger")));
    }

    @Test
    public void testEncodePolicy() {
        final String encodedPolicy = RetentionPolicyCodec.INSTANCE.encodePolicy(
                MockRetentionPolicyType.INSTANCE,
                Map.of("foo", "bar", "baz", "bop", "bad key", "whatever"));
        assertEquals("mock-policy baz=bop foo=bar", encodedPolicy);
    }

    @Test
    public void testDecodePolicy() {
        final RetentionPolicy policy = RetentionPolicyCodec.INSTANCE.decodePolicy(
                List.of(MockRetentionPolicyType.INSTANCE),
                "mock-policy foo=bar baz=bop random junk should be ignored"
        );
        assertTrue(policy instanceof MockRetentionPolicy);
        assertEquals(((MockRetentionPolicy) policy).config, Map.of("foo", "bar", "baz", "bop"));
    }

    @Test
    public void testEncodeMap() {
        String encoded = RetentionPolicyCodec.encodeMap(
                Map.of("foo", "bar", "baz", "bop", "bad key", "whatever"));
        assertEquals("baz=bop foo=bar", encoded);
    }

    @Test
    public void testDecodeMap() {
        final String encoded = "foo=bar baz=bop random junk should be ignored";
        Map<String, String> decoded = RetentionPolicyCodec.decodeMap(encoded);
        assertEquals(Map.of("foo", "bar", "baz", "bop"), decoded);
    }

    private static class MockRetentionPolicy implements RetentionPolicy {

        private final Map<String, String> config;

        public MockRetentionPolicy(Map<String, String> config) {
            this.config = config;
        }

        @Override
        public UserMessage getDescription() {
            return UserMessage.raw("mock policy");
        }

        @Override
        public Collection<SnapshotId> getSnapshotsToPrune(Set<SnapshotId> fromSnapshots) {
            throw new IllegalStateException();
        }
    }

    private enum MockRetentionPolicyType implements RetentionPolicyType {

        INSTANCE;

        @Override
        public String getName() {
            return "mock-policy";
        }

        @Override
        public List<Parameter<?>> getParameters() {
            throw new IllegalStateException();
        }

        @Override
        public RetentionPolicy createPolicy(Map<String, String> config) {
            return new MockRetentionPolicy(config);
        }

        @Override
        public UserMessage getDescription() {
            return UserMessage.raw("mock retention policy");
        }
    }

    ;
}

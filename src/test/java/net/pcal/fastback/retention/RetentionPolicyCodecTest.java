package net.pcal.fastback.retention;

import net.pcal.fastback.MockModContext;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.utils.SnapshotId;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RetentionPolicyCodecTest {

    @Test
    public void testEncodePolicy() {
        final String encodedPolicy = RetentionPolicyCodec.INSTANCE.encodePolicy(
                MockModContext.create(),
                MockRetentionPolicyType.INSTANCE,
                Map.of("foo", "bar", "baz","bop", "bad key", "whatever"));
        assertEquals("mock-policy baz=bop foo=bar", encodedPolicy);
    }

    @Test
    public void testDecodePolicy() {
        final RetentionPolicy policy = RetentionPolicyCodec.INSTANCE.decodePolicy(
                MockModContext.create(),
                List.of(MockRetentionPolicyType.INSTANCE),
                "mock-policy foo=bar baz=bop random junk should be ignored"
        );
        assertTrue(policy instanceof MockRetentionPolicy);
        assertEquals(((MockRetentionPolicy)policy).config, Map.of("foo", "bar", "baz", "bop"));
    }

    @Test
    public void testEncodeMap() {
        String encoded = RetentionPolicyCodec.encodeMap(MockModContext.create(),
                Map.of("foo", "bar", "baz","bop", "bad key", "whatever"));
        assertEquals("baz=bop foo=bar", encoded);
    }

    @Test
    public void testDecodeMap() {
        final String encoded = "foo=bar baz=bop random junk should be ignored";
        Map<String, String> decoded = RetentionPolicyCodec.decodeMap(MockModContext.create(), encoded);
        assertEquals(Map.of("foo", "bar", "baz", "bop"), decoded);
    }

    private static class MockRetentionPolicy implements RetentionPolicy {

        private final Map<String, String> config;

        public MockRetentionPolicy(Map<String, String> config) {
            this.config = config;
        }

        @Override
        public Collection<SnapshotId> getSnapshotsToPrune(Collection<SnapshotId> fromSnapshots) {
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
        public List<Parameter> getParameters() {
            throw new IllegalStateException();
        }

        @Override
        public RetentionPolicy createPolicy(ModContext ctx, Map<String, String> config) {
            return new MockRetentionPolicy(config);
        }
    };
}
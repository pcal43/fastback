package net.pcal.fastback.retention;

import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Message;
import net.pcal.fastback.utils.SnapshotId;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static net.pcal.fastback.logging.Message.localized;

/**
 * Policy to retain all snapshots.
 *
 * @author pcal
 * @since 0.1.5
 */
enum AllRetentionPolicy implements RetentionPolicy {

    INSTANCE;

    private static final String L10N_KEY = "fastback.retain.all.description";

    @Override
    public Message getDescription() {
        return localized(L10N_KEY);
    }

    @Override
    public Collection<SnapshotId> getSnapshotsToPrune(Collection<SnapshotId> fromSnapshots) {
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

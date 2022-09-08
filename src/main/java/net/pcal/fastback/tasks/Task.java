package net.pcal.fastback.tasks;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.tasks.Task.TaskState.COMPLETED;
import static net.pcal.fastback.tasks.Task.TaskState.FAILED;
import static net.pcal.fastback.tasks.Task.TaskState.NEW;
import static net.pcal.fastback.tasks.Task.TaskState.STARTED;

@SuppressWarnings("FieldCanBeLocal")
abstract public class Task implements Runnable {

    private TaskState state = null;
    private long startTime = -1;
    private long endTime = -1;
    private long splitStartTime = -1;

    public Task() {
        this.state = NEW;
    }

    protected void setState(TaskState newState) {
        requireNonNull(newState);
        if (!this.state.validTransistions.contains(newState)) {
            throw new IllegalStateException("invalid transition from " + this.state + " to " + newState);
        }
        this.state = newState;
        switch (this.state) {
            case COMPLETED:
            case FAILED:
                this.endTime = System.currentTimeMillis();
                break;
            case STARTED:
                this.startTime = System.currentTimeMillis();
                this.splitStartTime = this.startTime;
            case NEW:
                break;
        }
    }

    public boolean isFailed() {
        return this.state == FAILED;
    }
    public boolean isCompleted() {
        return this.state == COMPLETED;
    }

    public Duration getSplitDuration() {
        switch(this.state) {
            case COMPLETED:
            case FAILED:
                return Duration.ofMillis(this.endTime - this.splitStartTime);
            case STARTED:
                final long now = System.currentTimeMillis();
                final Duration out = Duration.ofMillis(now - this.splitStartTime);
                this.splitStartTime = now;
                return out;
        }
        throw new IllegalStateException("invalid state "+this.state);
    }

    public Duration getDuration() {
        if (this.state == COMPLETED || this.state == FAILED) {
            return Duration.ofMillis(this.endTime - this.startTime);
        } else {
            throw new IllegalStateException("Invalid state " + this.state);
        }
    }

    protected void setStarted() {
        setState(STARTED);
    }

    protected void setCompleted() {
        setState(COMPLETED);
    }

    protected void setFailed() {
        setState(FAILED);
    }

    public enum TaskState {
        COMPLETED(Collections.emptySet()),
        FAILED(Collections.emptySet()),
        STARTED(Set.of(COMPLETED, FAILED)),
        NEW(Set.of(STARTED));

        private final Collection<TaskState> validTransistions;
        TaskState(Collection<TaskState> validTransistions) {
            this.validTransistions = requireNonNull(validTransistions);
        }
    }
}

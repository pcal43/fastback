package net.pcal.fastback.tasks;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.tasks.Task.TaskState.COMPLETED;
import static net.pcal.fastback.tasks.Task.TaskState.FAILED;
import static net.pcal.fastback.tasks.Task.TaskState.NEW;
import static net.pcal.fastback.tasks.Task.TaskState.RUNNING;

@SuppressWarnings("FieldCanBeLocal")
abstract public class Task implements Runnable {

    private TaskState state = null;
    private long startTime = -1;
    private long endTime = -1;

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
            case RUNNING:
                this.startTime = System.currentTimeMillis();
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

    public Duration getDuration() {
        if (this.state == COMPLETED || this.state == FAILED) {
            return Duration.ofMillis(this.endTime - this.startTime);
        } else {
            throw new IllegalStateException("Invalid state " + this.state);
        }
    }

    protected void setStarted() {
        setState(RUNNING);
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
        RUNNING(Set.of(COMPLETED, FAILED)),
        NEW(Set.of(RUNNING));

        private final Collection<TaskState> validTransistions;
        TaskState(Collection<TaskState> validTransistions) {
            this.validTransistions = requireNonNull(validTransistions);
        }
    }
}

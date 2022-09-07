package net.pcal.fastback.tasks;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.tasks.Task.TaskState.NEW;

@SuppressWarnings("FieldCanBeLocal")
abstract public class Task implements Runnable {

    private TaskState state = null;
    public Task() {
        this.state = NEW;
    }

    protected void setState(TaskState state) {
        this.state = requireNonNull(state);
    }

    public TaskState getState() {
        return this.state;
    }

    public enum TaskState {
        NEW,
        RUNNING,
        COMPLETED,
        FAILED

    }
}

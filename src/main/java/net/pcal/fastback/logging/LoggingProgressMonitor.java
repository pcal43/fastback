package net.pcal.fastback.logging;

import org.eclipse.jgit.lib.ProgressMonitor;

import static java.util.Objects.requireNonNull;

public class LoggingProgressMonitor implements ProgressMonitor {

    private final Logger logger;
    private String currentTask;
    private int currentTotalWork;
    private int totalCompleted;

    public LoggingProgressMonitor(Logger logger) {
        this.logger = requireNonNull(logger);
    }

    @Override
    public void start(int totalTasks) {
    }

    @Override
    public void beginTask(String taskName, int totalWork) {
        this.currentTask = taskName;
        this.currentTotalWork = totalWork;
        this.totalCompleted = 0;
        this.logger.info(taskName);
    }

    @Override
    public void update(int completed) {
        this.totalCompleted += completed;
        int percent = (this.totalCompleted * 100) / this.currentTotalWork;
        this.logger.progressComplete(currentTask, percent);
    }

    @Override
    public void endTask() {
        this.logger.progressComplete(currentTask);
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}

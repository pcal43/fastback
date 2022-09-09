package net.pcal.gitback.progress;

import org.eclipse.jgit.lib.ProgressMonitor;

import static java.util.Objects.requireNonNull;

public class IncrementalProgressMonitor implements ProgressMonitor {

    private final ProgressMonitor delegate;
    private final int totalIncrements;
    private int workComplete;
    private int totalWork;
    private int workCompletedInIncrement;
    private int workCompleteScaled;

    public IncrementalProgressMonitor(ProgressMonitor delegate, int totalIncrements) {
        this.delegate = requireNonNull(delegate);
        this.totalIncrements = totalIncrements;
    }

    @Override
    public void start(int totalTasks) {
        this.delegate.start(totalTasks);
    }

    @Override
    public void beginTask(String taskName, int totalWork) {
        this.delegate.beginTask(taskName, totalWork == 0 ? 0 : totalWork);
        this.totalWork = totalWork;
        this.workComplete = 0;
        this.workCompleteScaled = 0;
        this.workCompletedInIncrement = 0;
    }

    @Override
    public void update(int completed) {
        this.workComplete += completed;
        this.workCompletedInIncrement += completed;
        if (this.totalWork == 0) return;
        int newWorkCompleteScaled = (this.workComplete * totalIncrements) / this.totalWork;
        if (newWorkCompleteScaled > this.workCompleteScaled) {
            this.workCompleteScaled = newWorkCompleteScaled;
            this.delegate.update(workCompletedInIncrement);
            this.workCompletedInIncrement = 0;
        }
    }

    @Override
    public void endTask() {
        this.delegate.endTask();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}

package net.pcal.gitback;

import org.eclipse.jgit.lib.ProgressMonitor;

import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

public class ScaledProgressMonitor implements ProgressMonitor {

    private final BiConsumer<String, Integer> sink;
    private final int scale;
    private int workComplete;
    private int totalWork;
    private int workCompleteScaled;
    private String currentTaskName;

    public ScaledProgressMonitor(BiConsumer<String, Integer> sink, int scale) {
        this.sink = requireNonNull(sink);
        this.scale = scale;
    }

    @Override
    public void start(int totalTasks) {
    }

    @Override
    public void beginTask(String taskName, int totalWork) {
        this.currentTaskName = taskName;
        this.sink.accept(taskName, 0);
        this.totalWork = totalWork;
        this.workComplete = 0;
        this.workCompleteScaled = 0;
    }

    @Override
    public void update(int completed) {
        this.workComplete += completed;
        if (this.totalWork == 0) return;
        int newWorkCompleteScaled = (this.workComplete * scale) / this.totalWork;
        if (newWorkCompleteScaled > this.workCompleteScaled) {
            this.workCompleteScaled = newWorkCompleteScaled;
            this.sink.accept(this.currentTaskName, this.workCompleteScaled);
        }
    }

    @Override
    public void endTask() {
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}

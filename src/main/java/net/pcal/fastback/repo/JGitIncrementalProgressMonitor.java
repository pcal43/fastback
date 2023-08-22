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

package net.pcal.fastback.repo;

import org.eclipse.jgit.lib.ProgressMonitor;

import static java.util.Objects.requireNonNull;

class JGitIncrementalProgressMonitor implements ProgressMonitor {

    private final ProgressMonitor delegate;
    private final int totalIncrements;
    private int workComplete;
    private int totalWork;
    private int workCompletedInIncrement;
    private int workCompleteScaled;

    public JGitIncrementalProgressMonitor(ProgressMonitor delegate, int totalIncrements) {
        this.delegate = requireNonNull(delegate);
        this.totalIncrements = totalIncrements;
    }

    @Override
    public void start(int totalTasks) {
        this.delegate.start(totalTasks);
    }

    @Override
    public void beginTask(String taskName, int totalWork) {
        this.delegate.beginTask(taskName, totalWork);
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

    @Override
    public void showDuration(boolean enabled) {
    }
}

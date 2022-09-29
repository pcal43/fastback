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

package net.pcal.fastback.progress;

import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.lib.ProgressMonitor;

import static java.util.Objects.requireNonNull;

public abstract class PercentageProgressMonitor implements ProgressMonitor {

    private String currentTask;
    private int currentTotalWork;
    private int totalCompleted;

    public PercentageProgressMonitor() {
    }

    @Override
    public void start(int totalTasks) {
    }

    @Override
    public void beginTask(String taskName, int totalWork) {
        this.currentTask = taskName;
        this.currentTotalWork = totalWork;
        this.totalCompleted = 0;
    }

    @Override
    public void update(int completed) {
        this.totalCompleted += completed;
        int percent = (this.totalCompleted * 100) / this.currentTotalWork;
        this.progressComplete(currentTask, percent);
    }

    @Override
    public void endTask() {
        this.progressComplete(currentTask);
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}

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

package net.pcal.fastback.commands;


import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Logger;

import static java.util.Objects.requireNonNull;


public enum SchedulableAction {

    LOCAL("local") {

        @Override
        public void run(ModContext ctx, ServerCommandSource scs, Logger log) {
            log.info("Starting scheduled local backup");
            //new CommitTask(git, ctx, server, log).run();
        }
    };

    public static SchedulableAction getForConfigKey(String configKey) {
        return LOCAL; //FIXME
    }

    private final String configKey;

    SchedulableAction(String configKey) {
        this.configKey = requireNonNull(configKey);
    }

    public String getConfigKey() {
        return this.configKey;
    }

    public abstract void run(ModContext ctx, ServerCommandSource scs, Logger log);
}

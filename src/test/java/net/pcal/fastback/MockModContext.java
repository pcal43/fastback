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

package net.pcal.fastback;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.pcal.fastback.logging.Log4jLogger;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.Message;
import net.pcal.fastback.retention.RetentionPolicyType;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Path;

public class MockModContext {

    public static ModContext create() {
        return ModContext.create(new MockFrameworkSpi());
    }

    private static class MockFrameworkSpi implements ModContext.FrameworkServiceProvider {

        private final Log4jLogger logger;

        public MockFrameworkSpi() {
            this.logger = new Log4jLogger(LogManager.getLogger("mocklogger"), String::valueOf);
        }

        @Override
        public Logger getLogger() {
            return this.logger;
        }

        @Override
        public String getModId() {
            return "mockmod";
        }

        @Override
        public String getModVersion() {
            return "1.2.3";
        }

        @Override
        public Path getConfigDir() {
            return null;
        }

        @Override
        public String getMinecraftVersion() {
            return "1.20.99";
        }

        @Override
        public Path getWorldDirectory(MinecraftServer server) {
            return null;
        }

        @Override
        public String getWorldName(MinecraftServer server) {
            return "MockWorld";
        }

        @Override
        public void setClientSavingScreenText(Message message) {

        }

        @Override
        public void sendClientChatMessage(Message message) {
        }

        @Override
        public Path getClientSavesDir() {
            return null;
        }

        @Override
        public boolean isClient() {
            return false;
        }

        @Override
        public boolean isWorldSaveEnabled() {
            return false;
        }

        @Override
        public void setWorldSaveEnabled(boolean enabled) {

        }

        @Override
        public void sendFeedback(Message message, ServerCommandSource scs) {

        }

        @Override
        public void sendError(Message message, ServerCommandSource scs) {

        }
    }

}

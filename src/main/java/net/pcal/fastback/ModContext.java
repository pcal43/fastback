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
import net.minecraft.text.Text;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.retention.DailyRetentionPolicyType;
import net.pcal.fastback.retention.FixedCountRetentionPolicyType;
import net.pcal.fastback.retention.RetentionPolicyType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Objects.requireNonNull;

public class ModContext {

    private final FrameworkServiceProvider spi;
    private final ExecutorService exs;
    private Path tempRestoresDirectory = null;

    public static ModContext create(FrameworkServiceProvider spi) {
        final ExecutorService exs = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        return new ModContext(spi, exs);
    }

    private ModContext(FrameworkServiceProvider spi, ExecutorService exs) {
        this.spi = requireNonNull(spi);
        this.exs = requireNonNull(exs);
    }

    public ExecutorService getExecutorService() {
        return this.exs;
    }

    public String getCommandName() {
        return "backup"; // TODO i18n?
    }

    public Path getRestoresDir() throws IOException {
        Path restoreDir = this.spi.getClientSavesDir();
        if (restoreDir != null) return restoreDir;
        if (tempRestoresDirectory == null) {
            tempRestoresDirectory = createTempDirectory(getModId() + "-restore");
        }
        return tempRestoresDirectory;
    }

    // PASSTHROUGH IMPLEMENTATIONS

    String getModId() {
        return this.spi.getModId();
    }

    public String getModVersion() {
        return this.spi.getModVersion();
    }

    public String getMinecraftVersion() {
        return this.spi.getMinecraftVersion();
    }

    public Path getMinecraftConfigDir() {
        return this.spi.getConfigDir();
    }

    public void setWorldSaveEnabled(boolean enabled) {
        this.spi.setWorldSaveEnabled(enabled);
    }

    public boolean isClient() {
        return spi.isClient();
    }

    public void setSavingScreenText(Text text) {
        this.spi.setClientSavingScreenText(text);
    }

    public void sendClientChatMessage(Text text) {
        this.spi.sendClientChatMessage(text);
    }

    public Path getWorldSaveDirectory(MinecraftServer server) {
        return this.spi.getWorldDirectory(server);
    }

    public boolean isWorldSaveEnabled() {
        return this.spi.isWorldSaveEnabled();
    }

    public String getWorldName(MinecraftServer server) {
        return this.spi.getWorldName(server);
    }

    public Logger getLogger() {
        return this.spi.getLogger();
    }

    // TODO make these configurable via properties

    public boolean isExperimentalCommandsEnabled() {
        return false;
    }

    public boolean isStartupNotificationEnabled() {
        return true;
    }

    public boolean isCommandDumpEnabled() {
        return true;
    }

    public boolean isReflogDeletionEnabled() {
        return true;
    }

    public int getDefaultPermLevel() {
        return spi.isClient() ? 0 : 4;
    }

    public List<RetentionPolicyType> getAvailableRetentionPolicyTypes() {
        return List.of(DailyRetentionPolicyType.INSTANCE, FixedCountRetentionPolicyType.INSTANCE);
    }

    public TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

    public interface FrameworkServiceProvider {

        Logger getLogger();

        String getModId();

        String getModVersion();

        Path getConfigDir();

        String getMinecraftVersion();

        Path getWorldDirectory(MinecraftServer server);

        String getWorldName(MinecraftServer server);

        void setClientSavingScreenText(Text text);

        void sendClientChatMessage(Text text);

        Path getClientSavesDir() throws IOException;

        boolean isClient();

        boolean isWorldSaveEnabled();

        void setWorldSaveEnabled(boolean enabled);
    }
}

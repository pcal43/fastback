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
package net.pcal.fastback.mod;

import net.minecraft.commands.CommandSourceStack;
import net.pcal.fastback.commands.SchedulableAction;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.RepoFactory;
import org.eclipse.jgit.transport.SshSessionFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.config.FastbackConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.SHUTDOWN_ACTION;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.localized;
import static net.pcal.fastback.utils.EnvironmentUtils.getGitLfsVersion;
import static net.pcal.fastback.utils.EnvironmentUtils.getGitVersion;
import static net.pcal.fastback.utils.Executor.executor;

class ModImpl implements LifecycleListener, Mod {

    // ======================================================================
    // Fields

    private final MinecraftProvider fsp;
    private Path tempRestoresDirectory = null;

    // ======================================================================
    // Construction

    ModImpl(final MinecraftProvider spi) {
        this.fsp = requireNonNull(spi);
        spi.setAutoSaveListener(new AutosaveListener());
    }

    // ======================================================================
    // Mod implementation

    @Override
    public Path getDefaultRestoresDir() throws IOException {
        Path restoreDir = this.fsp.getSavesDir();
        if (restoreDir != null) return restoreDir;
        if (tempRestoresDirectory == null) {
            tempRestoresDirectory = createTempDirectory("fastback-restore");
        }
        return tempRestoresDirectory;
    }

    @Override
    public void sendChat(UserMessage message, CommandSourceStack scs) {
        fsp.sendChat(message, scs);
    }

    @Override
    public void sendBroadcast(UserMessage message) {
        this.fsp.sendBroadcast(message);
    }

    // ======================================================================
    // Mod implementation passthroughs

    @Override
    public String getModVersion() {
        return this.fsp.getModVersion();
    }

    @Override
    public void setWorldSaveEnabled(boolean enabled) {
        this.fsp.setWorldSaveEnabled(enabled);
    }

    @Override
    public void setMessageScreenText(UserMessage message) {
        this.fsp.setMessageScreenText(message);
    }

    @Override
    public void setHudText(UserMessage message) {
        if (message == null) {
            syslog().debug("null unexpectedly passed to setHudText, ignoring");
            this.clearHudText();
        } else {
            this.fsp.setHudText(message);
        }
    }

    @Override
    public void clearHudText() {
        this.fsp.clearHudText();
    }

    @Override
    public Path getWorldDirectory() {
        return this.fsp.getWorldDirectory();
    }

    @Override
    public String getWorldName() {
        return this.fsp.getWorldName();
    }

    @Override
    public void addBackupProperties(Map<String, String> props) {
        fsp.addBackupProperties(props);
    }

    @Override
    public void saveWorld() {
        this.fsp.saveWorld();
    }

    @Override
    public Collection<Path> getModsBackupPaths() {
        return fsp.getModsBackupPaths();
    }

    // ======================================================================
    // LifecycleListener implementation

    /**
     * Must be called early in initialization of either a client or server.
     */
    @Override
    public void onInitialize() {
        {
            final String gitVersion = getGitVersion();
            if (gitVersion == null) {
                syslog().warn("git is not installed.");
            } else {
                syslog().info("git is installed: " + gitVersion);
            }
        }
        {
            final String gitLfsVersion = getGitLfsVersion();
            if (gitLfsVersion == null) {
                syslog().warn("git-lfs is not installed.");
            } else {
                syslog().info("git-lfs is installed: " + gitLfsVersion);
            }
        }
        if (SshSessionFactory.getInstance() == null) {
            syslog().warn("An ssh provider was not initialized for jgit.  Operations on a remote repo over ssh will fail.");
        } else {
            syslog().info("SshSessionFactory: " + SshSessionFactory.getInstance().toString());
        }
        syslog().debug("onInitialize complete");
    }

    /**
     * Must be called when a world is starting (in either a dedicated or client-embedded server).
     */
    @Override
    public void onWorldStart() {
        executor().start();
        syslog().debug("onWorldStart complete");
    }

    /**
     * Must be called when a world is stopping (in either a dedicated or client-embedded server).
     */
    @Override
    public void onWorldStop() {
        try (final UserLogger ulog = UserLogger.forShutdown()) {
            final Path worldSaveDir = this.getWorldDirectory();
            if (executor().getActiveCount() > 0) {
                this.setMessageScreenText(localized("fastback.chat.thread-waiting"));
            }
            executor().stop();
            this.clearHudText();
            final RepoFactory rf = RepoFactory.rf();
            if (rf.isGitRepo(worldSaveDir)) {
                try (final Repo repo = rf.load(worldSaveDir)) {
                    final GitConfig config = repo.getConfig();
                    if (config.getBoolean(IS_BACKUP_ENABLED)) {
                        final SchedulableAction action = SchedulableAction.forConfigValue(config, SHUTDOWN_ACTION);
                        if (action != null) {
                            this.setMessageScreenText(localized("fastback.message.backing-up"));
                            action.getTask(repo, ulog).call();
                            this.setMessageScreenText(localized("fastback.chat.backup-complete"));
                        }
                    }
                } catch (Exception e) {
                    syslog().error("Shutdown action failed.", e);
                }
            }
            syslog().debug("onWorldStop complete");
        }
    }


}

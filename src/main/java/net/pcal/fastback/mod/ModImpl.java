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

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.pcal.fastback.commands.Commands;
import net.pcal.fastback.commands.SchedulableAction;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.RepoFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Objects.requireNonNull;
import static net.minecraft.text.Style.EMPTY;
import static net.pcal.fastback.config.FastbackConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.SHUTDOWN_ACTION;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.BROADCAST;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.JGIT;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.NATIVE_GIT;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.WARNING;
import static net.pcal.fastback.logging.UserMessage.localized;
import static net.pcal.fastback.utils.EnvironmentUtils.getGitLfsVersion;
import static net.pcal.fastback.utils.EnvironmentUtils.getGitVersion;
import static net.pcal.fastback.utils.Executor.executor;

class ModImpl implements LifecycleListener, Mod {

    // ======================================================================
    // Fields

    private final FrameworkServiceProvider fsp;
    private Path tempRestoresDirectory = null;

    // ======================================================================
    // Construction

    ModImpl(final FrameworkServiceProvider spi) {
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
    public void sendChat(UserMessage message, ServerCommandSource scs) {
        if (message.style() == ERROR) {
            scs.sendError(messageToText(message));
        } else {
            scs.sendFeedback(() -> messageToText(message), false);
        }
    }

    @Override
    public void sendBroadcast(UserMessage message) {
        this.fsp.sendBroadcast(messageToText(message));
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
        this.fsp.setMessageScreenText(messageToText(message));
    }

    @Override
    public void setHudText(UserMessage message) {
        if (message == null) {
            syslog().debug("null unexpectedly passed to setHudText, ignoring");
            this.clearHudText();
        } else {
            this.fsp.setHudText(messageToText(message));
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
    public int getDefaultPermLevel() {
        return fsp.isClient() ? 0 : 4;
    }

    @Override
    public void addBackupProperties(Map<String, String> props) {
        fsp.addBackupProperties(props);
    }

    @Override
    public boolean isDecicatedServer() {
        return fsp.isDedicatedServer();
    }

    @Override
    public void saveWorld() {
        this.fsp.saveWorld();
    }

    // ======================================================================
    // LifecycleListener implementation

    /**
     * Must be called early in initialization of either a client or server.
     */
    @Override
    public void onInitialize() {
        Commands.registerCommands(this);
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
                        }
                    }
                } catch (Exception e) {
                    syslog().error("Shutdown action failed.", e);
                }
            }
            syslog().debug("onWorldStop complete");
        } finally {
            this.clearHudText();
        }
    }


    // ======================================================================
    // Private

    private static Text messageToText(final UserMessage m) {
        final MutableText out;
        if (m.localized() != null) {
            out = Text.translatable(m.localized().key(), m.localized().params());
        } else {
            out = Text.literal(m.raw());
        }
        if (m.style() == ERROR) {
            out.setStyle(EMPTY.withColor(TextColor.parse("red")));
        } else if (m.style() == WARNING) {
            out.setStyle(EMPTY.withColor(TextColor.parse("yellow")));
        } else if (m.style() == NATIVE_GIT) {
            out.setStyle(EMPTY.withColor(TextColor.parse("green")));
        } else if (m.style() == JGIT) {
            out.setStyle(EMPTY.withColor(TextColor.parse("gray")));
        } else if (m.style() == BROADCAST) {
            out.setStyle(EMPTY.withItalic(true));
        } else {
            out.setStyle(EMPTY.withColor(TextColor.parse("white")));
        }
        return out;
    }
}

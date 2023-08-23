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
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.pcal.fastback.commands.Commands;
import net.pcal.fastback.commands.SchedulableAction;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.logging.UserMessage.UserMessageStyle;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.RepoFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.commands.SchedulableAction.NONE;
import static net.pcal.fastback.commands.SchedulableAction.forConfigValue;
import static net.pcal.fastback.config.GitConfigKey.AUTOBACK_ACTION;
import static net.pcal.fastback.config.GitConfigKey.AUTOBACK_WAIT_MINUTES;
import static net.pcal.fastback.config.GitConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.SHUTDOWN_ACTION;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.localized;
import static net.pcal.fastback.utils.EnvironmentUtils.getGitLfsVersion;
import static net.pcal.fastback.utils.EnvironmentUtils.getGitVersion;

public class ModContext implements ModLifecycleListener {

    private final FrameworkServiceProvider spi;
    private Path tempRestoresDirectory = null;
    private final Executor executor;

    public static ModContext create(FrameworkServiceProvider spi) {
        return new ModContext(spi);
    }

    private ModContext(FrameworkServiceProvider spi) {
        this.spi = requireNonNull(spi);
        spi.setAutoSaveListener(new AutosaveListener());
        this.executor = new Executor();
    }


    // ======================================================================
    // ModLifecycleListener implementation

    /**
     * Must be called early in initialization of either a client or server.
     */
    @Override
    public void onInitialize() {
        Commands.registerCommands(this, this.getCommandName());
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
        executor.start();
        syslog().debug("onWorldStart complete");
    }

    /**
     * Must be called when a world is stopping (in either a dedicated or client-embedded server).
     */
    @Override
    public void onWorldStop() {
        final Path worldSaveDir = this.getWorldDirectory();
        this.setHudText(localized("fastback.chat.thread-waiting"));
        executor.stop();
        this.setHudText(null);
        final RepoFactory rf = RepoFactory.get();
        if (rf.isGitRepo(worldSaveDir)) {
            try (final Repo repo = rf.load(worldSaveDir, this)) {
                final GitConfig config = repo.getConfig();
                if (config.getBoolean(IS_BACKUP_ENABLED)) {
                    final SchedulableAction action = SchedulableAction.forConfigValue(config, SHUTDOWN_ACTION);
                    if (action != null) {
                        this.setMessageScreenText(localized("fastback.message.backing-up"));
                        action.getTask(repo, new HudLogger(this)).call();
                    }
                }
            } catch (Exception e) {
                syslog().error("Shutdown action failed.", e);
            }
        }
        syslog().debug("onWorldStop complete");
    }

    public Executor getExecutor() {
        return this.executor;
    }

    class AutosaveListener implements Runnable {

        private long lastBackupTime = System.currentTimeMillis();

        @Override
        public void run() {
            //TODO implement indicator
            // final Logger screenLogger = CompositeLogger.of(ctx.getLogger(), new SaveScreenLogger(ctx));
            executor.execute(Executor.ExecutionLock.WRITE, new HudLogger(ModContext.this), () -> {
                RepoFactory rf = RepoFactory.get();
                final Path worldSaveDir = getWorldDirectory();
                if (!rf.isGitRepo(worldSaveDir)) return;
                try (final Repo repo = rf.load(worldSaveDir, ModContext.this)) {
                    final GitConfig config = repo.getConfig();
                    if (!config.getBoolean(IS_BACKUP_ENABLED)) return;
                    final SchedulableAction autobackAction = forConfigValue(config, AUTOBACK_ACTION);
                    if (autobackAction == null || autobackAction == NONE) return;
                    final Duration waitTime = Duration.ofMinutes(config.getInt(AUTOBACK_WAIT_MINUTES));
                    final Duration timeRemaining = waitTime.
                            minus(Duration.ofMillis(System.currentTimeMillis() - lastBackupTime));
                    if (!timeRemaining.isZero() && !timeRemaining.isNegative()) {
                        syslog().debug("Skipping auto-backup until at least " +
                                (timeRemaining.toSeconds() / 60) + " more minutes have elapsed.");
                        return;
                    }
                    syslog().info("Starting auto-backup");
                    autobackAction.getTask(repo, new HudLogger(ModContext.this));
                    lastBackupTime = System.currentTimeMillis();
                } catch (Exception e) {
                    syslog().error("auto-backup failed.", e);
                }
            });
        }
    }


    public String getCommandName() {
        return "backup"; // TODO i18n?
    }

    public Path getRestoresDir() throws IOException {
        Path restoreDir = this.spi.getSavesDir();
        if (restoreDir != null) return restoreDir;
        if (tempRestoresDirectory == null) {
            tempRestoresDirectory = createTempDirectory("fastback-restore");
        }
        return tempRestoresDirectory;
    }

    // PASSTHROUGH IMPLEMENTATIONS

    public String getModVersion() {
        return this.spi.getModVersion();
    }

    public void setWorldSaveEnabled(boolean enabled) {
        this.spi.setWorldSaveEnabled(enabled);
    }

    public void setMessageScreenText(UserMessage message) {
        this.spi.setMessageScreenText(messageToText(message));
    }

    public void sendChat(UserMessage message, ServerCommandSource scs) {
        if (message.style() == ERROR) {
            scs.sendError(messageToText(message));
        } else {
            scs.sendFeedback(() -> messageToText(message), false);
        }
    }

    public void setHudText(UserMessage message) {
        this.spi.setHudText(message == null ? null : messageToText(message));
    }

    public Path getWorldDirectory() {
        return this.spi.getWorldDirectory();
    }

    public String getWorldName() {
        return this.spi.getWorldName();
    }

    public int getDefaultPermLevel() {
        return spi.isClient() ? 0 : 4;
    }

    public void saveWorld() {
        this.spi.saveWorld();
    }


    private static Text messageToText(final UserMessage m) {
        final MutableText out;
        if (m.styledLocalized() != null) {
            out = Text.translatable(m.styledLocalized().key(), m.styledLocalized().params());
        } else {
            out = Text.literal(m.styledRaw());
        }
        if (m.style() == UserMessageStyle.ERROR) {
            out.setStyle(Style.EMPTY.withColor(TextColor.parse("red")));
        } else if (m.style() == UserMessageStyle.WARNING) {
            out.setStyle(Style.EMPTY.withColor(TextColor.parse("yellow")));
        } else if (m.style() == UserMessageStyle.NATIVE_GIT) {
            out.setStyle(Style.EMPTY.withColor(TextColor.parse("green")));
        }
        return out;
    }
}

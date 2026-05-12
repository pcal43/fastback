/*
 * FastBack - Fast, incremental Minecraft backups powered by Git.
 * Copyright (C) 2026 pcal.net
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
package net.pcal.fastback.neoforge;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.pcal.fastback.common.commands.PermissionsFactory;
import net.pcal.fastback.common.mod.LoaderHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static net.pcal.fastback.common.logging.SystemLogger.syslog;

/**
 * NeoForge implementation of {@link LoaderHelper}.
 *
 * @author pcal
 */
class NeoForgeLoaderHelper implements LoaderHelper {

    static final String NEOFORGE_MOD_ID = "fastback";

    private final boolean isClient;

    NeoForgeLoaderHelper(boolean isClient) {
        this.isClient = isClient;
    }

    @Override
    public String getModVersion() {
        return ModList.get().getModContainerById(NEOFORGE_MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElseThrow(() -> new IllegalStateException("Could not find mod container for " + NEOFORGE_MOD_ID));
    }

    @Override
    public void addLoaderBackupProperties(Map<String, String> props) {
        try {
            final List<String> modList = new ArrayList<>();
            ModList.get().getMods().forEach(info ->
                    modList.add(info.getModId() + ':' + info.getVersion()));
            Collections.sort(modList);
            final StringBuilder modListProp = new StringBuilder();
            for (final String mod : modList) modListProp.append(mod).append(", ");
            props.put("neoforge-mods", modListProp.toString());
        } catch (Exception ohwell) {
            syslog().error(ohwell);
        }
    }

    @Override
    public Path getSavesDir() {
        return isClient ? FMLPaths.GAMEDIR.get().resolve("saves") : null;
    }

    @Override
    public Collection<Path> getModsBackupPaths() {
        final Path gameDir = FMLPaths.GAMEDIR.get();
        final List<Path> out = new ArrayList<>();
        out.add(gameDir.resolve("options.txt"));
        out.add(gameDir.resolve("mods"));
        out.add(gameDir.resolve("config"));
        out.add(gameDir.resolve("resourcepacks"));
        return out;
    }

    @Override
    public void registerBackupCommand(boolean isForClient,
                                      Function<PermissionsFactory<CommandSourceStack>, LiteralArgumentBuilder<CommandSourceStack>> builder) {
        final int requiredLevel = isForClient ? 0 : 4;
        final PermissionLevel permLevel = PermissionLevel.byId(requiredLevel);
        final LiteralArgumentBuilder<CommandSourceStack> backupCommand =
                builder.apply(permName -> source -> source.permissions().hasPermission(new Permission.HasCommandLevel(permLevel)));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            event.getDispatcher().register(backupCommand);
            syslog().debug("registered backup command");
        });
    }
}

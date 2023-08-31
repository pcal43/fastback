package net.pcal.fastback.mod.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.lang.reflect.InvocationTargetException;

/**
 * @author pcal
 * @since 0.16.0
 */
@Mod("fastback")
final public class ForgeInitializer {

    public ForgeInitializer() {
        try {
            if (FMLEnvironment.dist.isDedicatedServer()) {
                new ForgeCommonProvider();
            } else if (FMLEnvironment.dist.isClient()) {
                // Forge yells at us if we touch any client classes in a server.  So,
                Class.forName("net.pcal.fastback.mod.forge.ForgeClientProvider").getConstructor().newInstance();
            } else {
                throw new IllegalStateException("where am i?  server or client?");
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
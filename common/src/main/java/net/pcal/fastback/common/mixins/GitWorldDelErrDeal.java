package net.pcal.fastback.common.mixins;

import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;


@Mixin(LevelStorageSource.LevelStorageAccess.class)
public abstract class GitWorldDelErrDeal {
    @Final
    @Shadow
    private LevelStorageSource.LevelDirectory levelDirectory;
    @Inject(
            method = "deleteLevel",
            at = @At("HEAD")
    )
    public void deleteLevelwithDealErr(CallbackInfo ci) throws Exception {
        Logger LOGGER=LevelStorageSourceAccessor.getLogger();

        boolean isGitWorld=Files.isDirectory(this.levelDirectory.path().resolve(".git"));
        if(isGitWorld){
            Files.walkFileTree(levelDirectory.path(),new SimpleFileVisitor<Path>(){
                public @NonNull FileVisitResult visitFile(final @NonNull Path file, final @NonNull BasicFileAttributes attrs) throws IOException{
                    if(!file.toFile().canWrite()){
                        file.toFile().setWritable(true);
                        LOGGER.debug("{} has been set writable to execute del",file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            LOGGER.info("Has set git files writable.");
        }
    }
}




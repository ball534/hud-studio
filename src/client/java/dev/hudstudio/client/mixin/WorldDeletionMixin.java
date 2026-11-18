package dev.hudstudio.client.mixin;

import dev.hudstudio.client.navigation.Navigation;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.nio.file.Path;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public abstract class WorldDeletionMixin {
    @Shadow public abstract Path getLevelPath(LevelResource resource);

    @Inject(method="deleteLevel",at=@At("RETURN"))
    private void studio_deleteWorldMap(CallbackInfo ci) {
        Navigation.deleteWorldMap(getLevelPath(LevelResource.ROOT));
    }
}

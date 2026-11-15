package dev.hudstudio;

import dev.hudstudio.client.HudStudioClient;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
   modid = "hud-studio",
   name = "ball's hud studio",
   version = "1.8.0-forge1.8.9",
   clientSideOnly = true,
   acceptedMinecraftVersions = "[1.8.9]",
   acceptableRemoteVersions = "*"
)
public final class HudStudio {
   public static final String ID = "hud-studio";
   public static final Logger LOGGER = LogManager.getLogger("hud-studio");

   @EventHandler
   public void init(FMLInitializationEvent event) {
      HudStudioClient.init();
   }
}

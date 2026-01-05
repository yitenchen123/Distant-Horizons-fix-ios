package com.seibel.distanthorizons.fabric.mixins.client;

import com.seibel.distanthorizons.api.enums.config.EDhApiUpdateBranch;
import com.seibel.distanthorizons.common.commonMixins.DhUpdateScreenBase;
import com.seibel.distanthorizons.common.wrappers.gui.updater.UpdateModScreen;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.jar.installer.GitlabGetter;
import com.seibel.distanthorizons.core.jar.installer.ModrinthGetter;
import com.seibel.distanthorizons.core.jar.updater.SelfUpdater;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.IVersionConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * At the moment this is only used for the auto updater
 *
 * @author coolGi
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft
{
	@Unique
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	
	@Unique
	private ClientLevel lastLevel;
	
	
	
	#if MC_VER < MC_1_20_2
	#if MC_VER == MC_1_20_1
	@Redirect(
			method = "Lnet/minecraft/client/Minecraft;setInitialScreen(Lcom/mojang/realmsclient/client/RealmsClient;Lnet/minecraft/server/packs/resources/ReloadInstance;Lnet/minecraft/client/main/GameConfig$QuickPlayData;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
	)
	public void onOpenScreen(Minecraft instance, Screen guiScreen)
	{
	#else
	@Redirect(
			method = "<init>(Lnet/minecraft/client/main/GameConfig;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
	)
	public void onOpenScreen(Minecraft instance, Screen guiScreen)
	{
	#endif
		if (!Config.Client.Advanced.AutoUpdater.enableAutoUpdater.get()) // Don't do anything if the user doesn't want it
		{
			instance.setScreen(guiScreen); // Sets the screen back to the vanilla screen as if nothing ever happened
			return;
		}
		
		if (SelfUpdater.onStart())
		{
			try
			{
				instance.setScreen(new UpdateModScreen(
						new TitleScreen(false), // We don't want to use the vanilla title screen as it would fade the buttons
						(Config.Client.Advanced.AutoUpdater.updateBranch.get() == EDhApiUpdateBranch.STABLE ? ModrinthGetter.getLatestIDForVersion(SingletonInjector.INSTANCE.get(IVersionConstants.class).getMinecraftVersion()): GitlabGetter.INSTANCE.projectPipelines.get(0).get("sha"))
				));
				return;
			}
			catch (Exception e)
			{
				// info instead of error since this can be ignored and probably just means
				// there isn't a new DH version available
				LOGGER.info("Unable to show DH update screen, reason: ["+e.getMessage()+"].");
			}
		}
		
		// Sets the screen back to the vanilla screen as if nothing ever happened
		// if not done the game will crash
		instance.setScreen(guiScreen);
	}
	#endif
	
	#if MC_VER >= MC_1_20_2
	@Redirect(
			method = "Lnet/minecraft/client/Minecraft;onGameLoadFinished(Lnet/minecraft/client/Minecraft$GameLoadCookie;)V",
			at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V")
	)
	private void buildInitialScreens(Runnable runnable)
	{
		DhUpdateScreenBase.tryShowUpdateScreenAndRunAutoUpdateStartup(runnable);
		runnable.run();
	}
	#endif
	
	// Level load/unload is handled via renderer-driven loading for Immersive Portals
	// and via render hooks in MixinLevelRenderer. Avoid duplicating level
	// load/unload logic here to keep a single, consistent system.
	
	@Inject(at = @At("HEAD"), method = "close()V")
	public void close(CallbackInfo ci) { SelfUpdater.onClose(); }
	
	// Use a dedicated timer for cleanup instead of MC tick events
	@Unique
	private static final java.util.Timer CLIENT_CLEANUP_TIMER = com.seibel.distanthorizons.core.util.TimerUtil.CreateTimer("ClientLevelTickCleanup");

	@Unique
	private static final java.util.TimerTask CLIENT_CLEANUP_TASK = com.seibel.distanthorizons.core.util.TimerUtil.createTimerTask(() -> com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper.tickCleanup());

	static
	{
		// 20 ticks per second (50ms interval)
		CLIENT_CLEANUP_TIMER.scheduleAtFixedRate(CLIENT_CLEANUP_TASK, 0, 1000 / 20);
	}
	
}

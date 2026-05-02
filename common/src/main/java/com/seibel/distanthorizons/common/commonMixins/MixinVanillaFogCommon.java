package com.seibel.distanthorizons.common.commonMixins;

import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IImmersivePortalsAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;


import net.minecraft.client.Camera;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

#if MC_VER < MC_1_17_1
import net.minecraft.world.level.material.FluidState;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#elif MC_VER < MC_1_21_3
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#elif MC_VER < MC_1_21_6
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.client.renderer.FogParameters;
import org.joml.Vector4f;
import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#else
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.FogData;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
#endif


public class MixinVanillaFogCommon
{
	
	
	#if MC_VER < MC_1_21_6
	public static boolean cancelFog(Camera camera, FogRenderer.FogMode fogMode)
	#else
	public static boolean cancelFog()
	#endif
	{
		
		
		#if MC_VER < MC_1_21_6
		Entity entity = camera.getEntity();
		#elif MC_VER <= MC_1_21_10
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		Entity entity = camera.getEntity();
		#else
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		Entity entity = camera.entity();	
		#endif
		
		
		boolean cameraNotInFluid = cameraNotInFluid(camera);
		boolean isSpecialFog = (entity instanceof LivingEntity) && ((LivingEntity) entity).hasEffect(MobEffects.BLINDNESS);
		
		boolean cancelFog = !isSpecialFog;
		cancelFog = cancelFog && cameraNotInFluid;
		#if MC_VER < MC_1_21_6
		cancelFog = cancelFog && (fogMode == FogRenderer.FogMode.FOG_TERRAIN);
		#endif
		
		cancelFog = cancelFog && !SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class).isFogStateSpecial();
		cancelFog = cancelFog && !Config.Client.Advanced.Graphics.Fog.enableVanillaFog.get();
		
		
		
		IImmersivePortalsAccessor immersivePortals = ModAccessorInjector.INSTANCE.get(IImmersivePortalsAccessor.class);
		if (immersivePortals != null
			&& immersivePortals.isRenderingPortal())
		{
			cancelFog = false;
		}
		
		return cancelFog;
	}
	
	private static boolean cameraNotInFluid(Camera camera)
	{
		#if MC_VER < MC_1_17_1
		FluidState fluidState = camera.getFluidInCamera();
		boolean cameraNotInFluid = fluidState.isEmpty();
		#else
		FogType fogTypes = camera.getFluidInCamera();
		boolean cameraNotInFluid = fogTypes == FogType.NONE;
		#endif
		
		return cameraNotInFluid;
	}
	
	
}

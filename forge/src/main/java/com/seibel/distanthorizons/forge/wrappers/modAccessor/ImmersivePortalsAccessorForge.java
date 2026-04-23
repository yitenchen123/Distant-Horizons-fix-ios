package com.seibel.distanthorizons.forge.wrappers.modAccessor;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.ImmersivePortalsAbstractAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public class ImmersivePortalsAccessorForge extends ImmersivePortalsAbstractAccessor
{
	@Override
	protected Object getClientLevel()
	{
		return Minecraft.getInstance().level;
	}
	@Override
	protected Class<?> getLevelClass()
	{
		return Level.class;
	}
	@Override
	protected Iterable<?> getEntitiesForRendering()
	{
		return Minecraft.getInstance().level.entitiesForRendering();
	}
	@Override
	protected Supplier<?> getFrustumSupplier()
	{
		return Suppliers.memoize(() -> {
			Frustum frustum = new Frustum(
				ClientApi.RENDER_STATE.mcModelViewMatrix.createJomlMatrix(),
				RenderSystem.getProjectionMatrix()
			);
			
			Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
			frustum.prepare(cameraPos.x, cameraPos.y, cameraPos.z);
			
			return frustum;
		});
	}
	
}

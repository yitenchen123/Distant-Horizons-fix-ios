package com.seibel.distanthorizons.common.wrappers.modAccessor;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.ImmersivePortalsAbstractAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

#if MC_VER > MC_1_19_2
import org.joml.Matrix4f;
#else
import com.mojang.math.Matrix4f;
import com.seibel.distanthorizons.core.util.math.Mat4f;
#endif

#if MC_VER < MC_1_17_1
import java.lang.reflect.Field;
#endif

import java.util.function.Supplier;

public abstract class ImmersivePortalsAccessorCommon extends ImmersivePortalsAbstractAccessor
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
	
	private static Matrix4f getProjectionMatrix() {
		#if MC_VER > MC_1_16_5
		return RenderSystem.getProjectionMatrix();
		#else
		try {
			Class<?> renderStates = Class.forName("com.qouteall.immersive_portals.render.context_management.RenderStates");
			Field projectionMatrix = renderStates.getField("projectionMatrix");
			return (Matrix4f) projectionMatrix.get(null);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		#endif
	}
	
	#if MC_VER <= MC_1_19_2
	protected abstract Matrix4f convert(Mat4f matrix);
	#endif
	
	@Override
	protected Supplier<?> getFrustumSupplier()
	{
		return Suppliers.memoize(() -> {
			Frustum frustum = new Frustum(
				#if MC_VER > MC_1_19_2
				ClientApi.RENDER_STATE.mcModelViewMatrix.createJomlMatrix(),
				#else
				convert(ClientApi.RENDER_STATE.mcModelViewMatrix),
				#endif
				getProjectionMatrix()
			);
			
			Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
			frustum.prepare(cameraPos.x, cameraPos.y, cameraPos.z);
			
			return frustum;
		});
	}
	
}

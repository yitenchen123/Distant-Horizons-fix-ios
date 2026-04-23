package com.seibel.distanthorizons.common.render.blaze;

#if MC_VER <= MC_1_21_10
public class BlazeDhTerrainRenderer {}

#else

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeBufferRenderEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderPassEvent;
import com.seibel.distanthorizons.common.render.blaze.util.BlazeDhVertexFormatUtil;
import com.seibel.distanthorizons.common.render.blaze.util.BlazeUniformUtil;
import com.seibel.distanthorizons.common.render.blaze.wrappers.RenderPipelineBuilderWrapper;
import com.seibel.distanthorizons.common.render.blaze.wrappers.texture.BlazeTextureViewWrapper;
import com.seibel.distanthorizons.common.render.blaze.wrappers.uniform.BlazeLodUniformBufferWrapper;
import com.seibel.distanthorizons.common.render.blaze.wrappers.buffer.BlazeVertexBufferWrapper;
import com.seibel.distanthorizons.common.wrappers.misc.LightMapWrapper;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodBufferContainer;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.util.math.Vec3d;
import com.seibel.distanthorizons.core.util.math.Vec3f;
import com.seibel.distanthorizons.core.util.objects.SortedArraySet;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhTerrainRenderer;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.objects.IVertexBufferWrapper;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/** Renders rendering DH's LOD terrain. */
public class BlazeDhTerrainRenderer implements IDhTerrainRenderer
{
	public static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final GpuDevice GPU_DEVICE = RenderSystem.getDevice();
	private static final CommandEncoder COMMAND_ENCODER = GPU_DEVICE.createCommandEncoder();
	
	public static final BlazeDhTerrainRenderer INSTANCE = new BlazeDhTerrainRenderer();
	
	
	private RenderPipeline opaquePipeline;
	private RenderPipeline transparentPipeline;
	private boolean init = false;
	
	private GpuBuffer fragUniformBuffer;
	private GpuBuffer vertSharedUniformBuffer;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private BlazeDhTerrainRenderer() { }
	
	private void tryInit()
	{
		if (this.init)
		{
			return;
		}
		
		
		
		RenderPipelineBuilderWrapper pipelineBuilder = new RenderPipelineBuilderWrapper();
		{
			pipelineBuilder.withFaceCulling(true);
			pipelineBuilder.withDepthWrite(true);
			pipelineBuilder.withDepthTest(RenderPipelineBuilderWrapper.EDhDepthTest.LESS);
			pipelineBuilder.withColorWrite(true);
			pipelineBuilder.withPolygonMode(RenderPipelineBuilderWrapper.EDhPolygonMode.FILL);
			pipelineBuilder.withName("terrain");
			
			pipelineBuilder.withSampler("uLightMap");
			
			pipelineBuilder.withVertexShader("lod/blaze/vert");
			pipelineBuilder.withFragmentShader("lod/blaze/frag");
			
			pipelineBuilder.withUniformBuffer("vertUniqueUniformBlock");
			pipelineBuilder.withUniformBuffer("vertSharedUniformBlock");
			pipelineBuilder.withUniformBuffer("fragUniformBlock");
			
			VertexFormat vertexFormat = VertexFormat.builder()
				.add("vPosition", BlazeDhVertexFormatUtil.SHORT_XYZ_POS)
				.add("meta", BlazeDhVertexFormatUtil.META)
				.add("vColor", BlazeDhVertexFormatUtil.RGBA_UBYTE_COLOR)
				.add("irisMaterial", BlazeDhVertexFormatUtil.IRIS_MATERIAL)
				.add("irisNormal", BlazeDhVertexFormatUtil.IRIS_NORMAL)
				.add("paddingTwo", BlazeDhVertexFormatUtil.BYTE_PAD)
				.add("paddingThree", BlazeDhVertexFormatUtil.BYTE_PAD) // padding is to make sure the format is a multiple of 4
				.build();
			pipelineBuilder.withVertexFormat(vertexFormat);
			
			pipelineBuilder.withVertexMode(RenderPipelineBuilderWrapper.EDhVertexMode.TRIANGLES);
		}
		
		// opaque
		{
			pipelineBuilder.withoutBlend();
			this.opaquePipeline = pipelineBuilder.build();
		}
		
		// transparent
		{
			// TRANSLUCENT = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
			pipelineBuilder.withBlend(BlendFunction.TRANSLUCENT);
			this.transparentPipeline = pipelineBuilder.build();
		}
		
		this.init = true;
	}
	
	//endregion
	
	
	
	//========//
	// render //
	//========//
	//region
	
	@Override
	public void render(
		RenderParams renderEventParam, 
		boolean opaquePass,
		SortedArraySet<LodBufferContainer> bufferContainers,
		IProfilerWrapper profiler)
	{
		this.tryInit();
		
		try(IProfilerWrapper.IProfileBlock terrain_profile = profiler.push("terrain render"))
		{
			profiler.popPush("vert unique uniforms");
			{
				// create data //
				
				for (int lodIndex = 0; lodIndex < bufferContainers.size(); lodIndex++)
				{
					LodBufferContainer bufferContainer = bufferContainers.get(lodIndex);
					bufferContainer.uniformContainer.tryUpload();
				}
			}
			
			profiler.popPush("vert share uniforms");
			{
				Mat4f combinedMatrix = new Mat4f(renderEventParam.dhProjectionMatrix);
				combinedMatrix.multiply(renderEventParam.dhModelViewMatrix);
				
				float earthCurveRatio = Config.Client.Advanced.Graphics.Experimental.earthCurveRatio.get();
				if (earthCurveRatio < -1.0f || earthCurveRatio > 1.0f)
				{
					earthCurveRatio = /*6371KM*/ 6371000.0f / earthCurveRatio;
				}
				else
				{
					// disable curvature if the config value is between -1 and 1
					earthCurveRatio = 0.0f;
				}
				
				
				// upload data //
				
				int uniformBufferSize = new Std140SizeCalculator()
					.putInt() // uIsWhiteWorld
					
					.putFloat() // uWorldYOffset
					.putFloat() // uMircoOffset
					.putFloat() // uEarthRadius
					
					.putVec3() // uCameraPos
					.putMat4f() // uCombinedMatrix
					.get();
				
				ByteBuffer buffer = MemoryUtil.memAlloc(uniformBufferSize);
				buffer.order(ByteOrder.nativeOrder());
				Std140Builder.intoBuffer(buffer)
					.putInt(0) // uIsWhiteWorld
					
					.putFloat((float) renderEventParam.worldYOffset) // uWorldYOffset
					.putFloat(0.01f) // uMircoOffset // 0.01 block offset
					.putFloat(earthCurveRatio) // uEarthRadius
					
					.putVec3(
						(float) renderEventParam.exactCameraPosition.x,
						(float) renderEventParam.exactCameraPosition.y,
						(float) renderEventParam.exactCameraPosition.z) // uCameraPos
					.putMat4f(combinedMatrix.createJomlMatrix()) // uCombinedMatrix
					.get();
				
				this.vertSharedUniformBuffer = BlazeUniformUtil.createBuffer("vertSharedUniformBlock", uniformBufferSize, this.vertSharedUniformBuffer);
				GpuBufferSlice bufferSlice = new GpuBufferSlice(this.vertSharedUniformBuffer, 0, uniformBufferSize);
				
				COMMAND_ENCODER.writeToBuffer(bufferSlice, buffer);
				
				MemoryUtil.memFree(buffer);
			}
			
			profiler.popPush("set frag uniforms");
			{
				int uniformBufferSize = new Std140SizeCalculator()
					.putFloat() // uClipDistance
					.putFloat() // uNoiseIntensity
					.putInt() // uNoiseSteps
					.putInt() // uNoiseDropoff
					.putInt() // uDitherDhRendering
					.putInt() // uNoiseEnabled
					.get();
				
				
				// create data //
				
				float dhNearClipDistance = RenderUtil.getNearClipPlaneInBlocks();
				if (!Config.Client.Advanced.Debugging.lodOnlyMode.get())
				{
					// this added value prevents the near clip plane and discard circle from touching, which looks bad
					dhNearClipDistance += 16f;
				}
				
				
				// upload data //
				
				ByteBuffer buffer = MemoryUtil.memAlloc(uniformBufferSize);
				buffer.order(ByteOrder.nativeOrder());
				buffer = Std140Builder.intoBuffer(buffer)
					.putFloat(dhNearClipDistance) // uClipDistance
					.putFloat(Config.Client.Advanced.Graphics.NoiseTexture.noiseIntensity.get()) // uNoiseIntensity
					.putInt(Config.Client.Advanced.Graphics.NoiseTexture.noiseSteps.get()) // uNoiseSteps
					.putInt(Config.Client.Advanced.Graphics.NoiseTexture.noiseDropoff.get()) // uNoiseDropoff
					.putInt(Config.Client.Advanced.Graphics.Quality.ditherDhFade.get() ? 1 : 0) // uDitherDhRendering
					.putInt(Config.Client.Advanced.Graphics.NoiseTexture.enableNoiseTexture.get() ? 1 : 0) // uNoiseEnabled
					.get()
				;
				
				this.fragUniformBuffer = BlazeUniformUtil.createBuffer("fragUniformBlock", uniformBufferSize, this.fragUniformBuffer);
				GpuBufferSlice bufferSlice = new GpuBufferSlice(this.fragUniformBuffer, 0, uniformBufferSize);
				
				COMMAND_ENCODER.writeToBuffer(bufferSlice, buffer);
				MemoryUtil.memFree(buffer);
			}
			
			
			
			// render pass setup
			{
				profiler.popPush("rendering");
				
				ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeRenderPassEvent.class, renderEventParam);
				
				// create a render pass
				try (RenderPass renderPass = COMMAND_ENCODER.createRenderPass(
					this::getRenderPassName,
					BlazeDhMetaRenderer.INSTANCE.dhColorTextureWrapper.textureView,
					/*optionalClearColorAsInt*/ OptionalInt.empty(),
					BlazeDhMetaRenderer.INSTANCE.dhDepthTextureWrapper.textureView,
					/*optionalDepthValueAsDouble*/ OptionalDouble.empty())
				)
				{
					LightMapWrapper lightMapWrapper = (LightMapWrapper) renderEventParam.lightmap;
					BlazeTextureViewWrapper lightmapTextureViewWrapper = lightMapWrapper.getTextureViewWrapper();
					renderPass.bindTexture("uLightMap", lightmapTextureViewWrapper.textureView, lightmapTextureViewWrapper.textureSampler);
					
					// set pipeline
					renderPass.setPipeline(opaquePass ? this.opaquePipeline : this.transparentPipeline);
					
					// shared uniforms
					renderPass.setUniform("fragUniformBlock", this.fragUniformBuffer);
					renderPass.setUniform("vertSharedUniformBlock", this.vertSharedUniformBuffer);
					
					
					
					for (int lodIndex = 0; lodIndex < bufferContainers.size(); lodIndex++)
					{
						LodBufferContainer bufferContainer = bufferContainers.get(lodIndex);
						BlazeLodUniformBufferWrapper uniformWrapper = (BlazeLodUniformBufferWrapper) bufferContainer.uniformContainer;
						
						boolean columnBuilderDebugEnabled = Config.Client.Advanced.Debugging.ColumnBuilderDebugging.columnBuilderDebugEnable.get();
						if (columnBuilderDebugEnabled)
						{
							if (DhSectionPos.getDetailLevel(bufferContainer.pos) == Config.Client.Advanced.Debugging.ColumnBuilderDebugging.columnBuilderDebugDetailLevel.get()
								&& DhSectionPos.getX(bufferContainer.pos) == Config.Client.Advanced.Debugging.ColumnBuilderDebugging.columnBuilderDebugXPos.get()
								&& DhSectionPos.getZ(bufferContainer.pos) == Config.Client.Advanced.Debugging.ColumnBuilderDebugging.columnBuilderDebugZPos.get())
							{
								int breakpoint = 0;
							}
							else
							{
								continue;
							}
						}
						
						renderPass.setUniform("vertUniqueUniformBlock", uniformWrapper.gpuBuffer);
						
						
						
						// render each buffer
						IVertexBufferWrapper[] bufferWrapperList = opaquePass ? bufferContainer.vboOpaqueWrappers : bufferContainer.vboTransparentWrappers;
						for (int i = 0; i < bufferWrapperList.length; i++)
						{
							BlazeVertexBufferWrapper bufferWrapper = (BlazeVertexBufferWrapper) bufferWrapperList[i];
							if (!bufferWrapper.uploaded
								|| bufferWrapper.vertexCount == 0)
							{
								continue;
							}
							
							// fire render event
							{
								Vec3d camPos = renderEventParam.exactCameraPosition;
								Vec3f modelPos = new Vec3f(
									(float) (bufferContainer.minCornerBlockPos.getX() - camPos.x),
									(float) (bufferContainer.minCornerBlockPos.getY() - camPos.y),
									(float) (bufferContainer.minCornerBlockPos.getZ() - camPos.z));
								ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeBufferRenderEvent.class, new DhApiBeforeBufferRenderEvent.EventParam(renderEventParam, modelPos));
							}
							
							renderPass.setIndexBuffer(bufferWrapper.getIndexGpuBuffer(), VertexFormat.IndexType.INT);
							renderPass.setVertexBuffer(0, bufferWrapper.vertexGpuBuffer); // vertex buffer can only be "0" lol
							
							if (!bufferWrapper.vertexGpuBuffer.isClosed())
							{
								renderPass.drawIndexed(
									/*indexStart*/ 0,
									/*firstIndex*/0,
									/*indexCount*/bufferWrapper.indexCount,
									/*instanceCount*/1);
							}
						}
					}
					
				}
			}
		}
	}
	private String getIndexBufferName() { return "distantHorizons:LodIndexBuffer"; }
	private String getRenderPassName() { return "distantHorizons:McLodRenderer"; }
	
	//endregion
	
	
	
}
#endif
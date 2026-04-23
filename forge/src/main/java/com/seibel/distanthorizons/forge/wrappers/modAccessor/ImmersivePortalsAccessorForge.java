package com.seibel.distanthorizons.forge.wrappers.modAccessor;

import com.seibel.distanthorizons.common.wrappers.modAccessor.ImmersivePortalsAccessorCommon;

#if MC_VER <= MC_1_19_2
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.mojang.math.Matrix4f;
#endif

public class ImmersivePortalsAccessorForge extends ImmersivePortalsAccessorCommon
{
	#if MC_VER <= MC_1_19_2
	@Override
	protected Matrix4f convert(Mat4f matrix) {
		return new Matrix4f(matrix.getValuesAsArray());
	}
	#endif
	
}

package com.seibel.distanthorizons.fabric.wrappers.modAccessor;

import com.seibel.distanthorizons.common.wrappers.modAccessor.ImmersivePortalsAccessorCommon;

#if MC_VER <= MC_1_19_2
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.mojang.math.Matrix4f;
import com.seibel.distanthorizons.fabric.mixins.client.AccessorMatrix4f;
#endif

public class ImmersivePortalsAccessorFabric extends ImmersivePortalsAccessorCommon
{
	#if MC_VER <= MC_1_19_2
	@Override
	protected Matrix4f convert(Mat4f matrix) {
		Matrix4f returnMatrix = new Matrix4f();
		AccessorMatrix4f accessibleMatrix = (AccessorMatrix4f) returnMatrix;
		accessibleMatrix.setM00(matrix.m00);
		accessibleMatrix.setM01(matrix.m01);
		accessibleMatrix.setM02(matrix.m02);
		accessibleMatrix.setM03(matrix.m03);
		
		accessibleMatrix.setM10(matrix.m10);
		accessibleMatrix.setM11(matrix.m11);
		accessibleMatrix.setM12(matrix.m12);
		accessibleMatrix.setM13(matrix.m13);
		
		accessibleMatrix.setM20(matrix.m20);
		accessibleMatrix.setM21(matrix.m21);
		accessibleMatrix.setM22(matrix.m22);
		accessibleMatrix.setM23(matrix.m23);
		
		accessibleMatrix.setM30(matrix.m30);
		accessibleMatrix.setM31(matrix.m31);
		accessibleMatrix.setM32(matrix.m32);
		accessibleMatrix.setM33(matrix.m33);
		return returnMatrix;
	}
	#endif
	
}

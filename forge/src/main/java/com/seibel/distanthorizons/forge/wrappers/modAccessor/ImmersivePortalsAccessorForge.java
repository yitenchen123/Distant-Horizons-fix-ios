/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

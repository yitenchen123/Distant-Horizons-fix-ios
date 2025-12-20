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

package com.seibel.distanthorizons.common;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.logging.DhLogger;

/**
 * Runtime detection and compatibility handling for Immersive Portals
 */
public class ImmersivePortalsCompat
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static volatile Boolean isImmersivePortalsPresent = null;
	private static volatile Boolean isImmersivePortalsActive = null;
	
	/**
	 * Check if Immersive Portals is present in the mod environment
	 */
	public static boolean isImmersivePortalsPresent()
	{
		if (isImmersivePortalsPresent == null)
		{
			synchronized (ImmersivePortalsCompat.class)
			{
				if (isImmersivePortalsPresent == null)
				{
					try
					{
						// Try to load an Immersive Portals class
						Class.forName("qouteall.imm_ptl.core.IPMcHelper");
						isImmersivePortalsPresent = true;
						LOGGER.info("Immersive Portals detected - enabling compatibility features");
					}
					catch (ClassNotFoundException e)
					{
						isImmersivePortalsPresent = false;
						LOGGER.debug("Immersive Portals not detected - using standard level management");
					}
				}
			}
		}
		return isImmersivePortalsPresent;
	}
	
	/**
	 * Check if Immersive Portals compatibility should be active
	 * This checks both presence and configuration
	 */
	public static boolean isImmersivePortalsActive()
	{
		if (isImmersivePortalsActive == null)
		{
			synchronized (ImmersivePortalsCompat.class)
			{
				if (isImmersivePortalsActive == null)
				{
					// TODO: Add configuration check here
					isImmersivePortalsActive = isImmersivePortalsPresent();
				}
			}
		}
		return isImmersivePortalsActive;
	}
	
	/**
	 * Reset detection cache (useful for testing)
	 */
	public static void resetDetection()
	{
		synchronized (ImmersivePortalsCompat.class)
		{
			isImmersivePortalsPresent = null;
			isImmersivePortalsActive = null;
		}
	}
}

package com.seibel.distanthorizons.common.wrappers.level;

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.level.IServerKeyedClientLevel;
import com.seibel.distanthorizons.core.level.IKeyedClientLevelManager;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class KeyedClientLevelManager implements IKeyedClientLevelManager
{
	public static final KeyedClientLevelManager INSTANCE = new KeyedClientLevelManager();
	
	private static class KeyInfo {
		public final String serverKey;
		public final String levelKey;
		public KeyInfo(String serverKey, String levelKey) {
			this.serverKey = serverKey;
			this.levelKey = levelKey;
		}
	}
	
	/** Stores the server-provided keys indexed by dimension name for persistence. */
	private final Map<String, KeyInfo> keysByDimensionName = new ConcurrentHashMap<>();
	
	/** Cache for already keyed level wrappers to maintain object identity. */
	private final Map<ClientLevel, IServerKeyedClientLevel> keyedLevelsCache = Collections.synchronizedMap(new WeakHashMap<>());
	
	/** Allows to keep level manager enabled between loading different keyed levels */
	private volatile boolean enabled = false;
	
	
	
	
	//=============//
	// constructor //
	//=============//
	
	private KeyedClientLevelManager() { }
	
	
	
	//======================//
	// level override logic //
	//======================//
	
	@Override
	@Nullable
	public IServerKeyedClientLevel getServerKeyedLevel() 
	{ 
		return this.getServerKeyedLevel(Minecraft.getInstance().level); 
	}
	
	@Nullable
	public IServerKeyedClientLevel getServerKeyedLevel(@Nullable ClientLevel level)
	{
		if (level == null)
		{
			return null;
		}
		
		// We synchronize on the cache map to ensure atomicity of the lookup-and-populate sequence.
		// This prevents multiple threads from creating duplicate wrappers for the same level.
		synchronized (this.keyedLevelsCache)
		{
			// Check the cache first
			IServerKeyedClientLevel cached = this.keyedLevelsCache.get(level);
			if (cached != null)
			{
				return cached;
			}
			
			// Determine the dimension name for this level
			// We use bypassLevelKeyManager=true to avoid recursion back into this manager
			IClientLevelWrapper wrappedLevel = ClientLevelWrapper.getWrapper(level, true);
			if (wrappedLevel == null)
			{
				return null;
			}
			
			String dimensionName = wrappedLevel.getDimensionName();
			KeyInfo info = this.keysByDimensionName.get(dimensionName);
			if (info == null)
			{
				return null;
			}
			
			// Create and cache a new keyed wrapper
			IServerKeyedClientLevel keyedLevel = new ServerKeyedClientLevelWrapper(level, info.serverKey, info.levelKey);
			this.keyedLevelsCache.put(level, keyedLevel);
			return keyedLevel;
		}
	}
	
	@Override
	public IServerKeyedClientLevel setServerKeyedLevel(IClientLevelWrapper clientLevel, String serverKey, String levelKey)
	{
		// 1. Determine the target dimension name
		String targetDimensionName = clientLevel.getDimensionName();
		int separatorIndex = levelKey.lastIndexOf("@");
		if (separatorIndex != -1)
		{
			targetDimensionName = levelKey.substring(separatorIndex + 1);
		}
		
		final String finalTargetDimensionName = targetDimensionName;
		
		// 2. Store the key for this dimension
		this.keysByDimensionName.put(finalTargetDimensionName, new KeyInfo(serverKey, levelKey));
		this.enabled = true;
		
		// 3. Clear the cache for this dimension to ensure new wrappers are created with the new key
		// (though in practice keys shouldn't change mid-session)
		// 
		// We synchronize manually on the map to ensure atomicity of the compound removal operation
		// and to prevent race conditions or deadlocks with other threads accessing the map.
		// We avoid calling ClientLevelWrapper.getWrapper() inside the lock to prevent circular lock dependencies.
		synchronized (this.keyedLevelsCache)
		{
			this.keyedLevelsCache.keySet().removeIf(level -> {
				#if MC_VER <= MC_1_21_10
				String levelDim = level.dimension().location().toString();
				#else
				String levelDim = level.dimension().identifier().toString();
				#endif
				return levelDim.equals(finalTargetDimensionName);
			});
		}
		
		// 4. Return the keyed wrapper for whatever level the core passed us, 
		// but only if it matches the dimension we just keyed.
		return this.getServerKeyedLevel((ClientLevel) clientLevel.getWrappedMcObject());
	}
	
	@Override
	public void clearKeyedLevel() 
	{ 
		synchronized (this.keyedLevelsCache)
		{
			this.keyedLevelsCache.clear(); 
			this.keysByDimensionName.clear();
		}
	}
	
	@Override
	public boolean isEnabled() { return this.enabled; }
	
	@Override
	public void disable() 
	{ 
		this.enabled = false; 
		this.clearKeyedLevel();
	}
	
}

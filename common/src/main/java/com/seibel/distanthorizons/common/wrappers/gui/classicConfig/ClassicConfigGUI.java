package com.seibel.distanthorizons.common.wrappers.gui.classicConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.seibel.distanthorizons.core.config.types.*;

import com.seibel.distanthorizons.core.config.types.enums.EConfigCommentTextPosition;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.config.IConfigGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.jetbrains.annotations.NotNull;


#if MC_VER < MC_1_20_1
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
#elif MC_VER <= MC_1_21_11
import net.minecraft.client.gui.GuiGraphics;
#else
import net.minecraft.client.gui.GuiGraphicsExtractor;
#endif

#if MC_VER >= MC_1_17_1
import net.minecraft.client.gui.narration.NarratableEntry;
#endif

#if MC_VER <= MC_1_21_10
#else
import net.minecraft.resources.Identifier;
#endif

import static com.seibel.distanthorizons.common.wrappers.gui.GuiHelper.*;


/*
 * Based upon TinyConfig but is highly modified
 * https://github.com/Minenash/TinyConfig
 *
 * @author coolGi
 * @author Motschen
 * @author James Seibel
 * @version 5-21-2022
 */
@SuppressWarnings("unchecked")
public class ClassicConfigGUI
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	public static final DhLogger RATE_LIMITED_LOGGER = new DhLoggerBuilder()
			.maxCountPerSecond(1)
			.build();
	
	public static final ConfigCoreInterface CONFIG_CORE_INTERFACE = new ConfigCoreInterface();
	
	
	
	//==============//
	// Initializers //
	//==============//
	
	// Some regexes to check if an input is valid
	public static final Pattern INTEGER_ONLY_REGEX = Pattern.compile("(-?[0-9]*)");
	public static final Pattern DECIMAL_ONLY_REGEX = Pattern.compile("-?([\\d]+\\.?[\\d]*|[\\d]*\\.?[\\d]+|\\.)");
	
	public static class ConfigScreenConfigs
	{
		// This contains all the configs for the configs
		public static final int SPACE_FROM_RIGHT_SCREEN = 10;
		public static final int SPACE_BETWEEN_TEXT_AND_OPTION_FIELD = 8;
		public static final int BUTTON_WIDTH_SPACING = 5;
		public static final int RESET_BUTTON_WIDTH = 60;
		public static final int RESET_BUTTON_HEIGHT = 20;
		public static final int OPTION_FIELD_WIDTH = 150;
		public static final int OPTION_FIELD_HEIGHT = 20;
		public static final int CATEGORY_BUTTON_WIDTH = 200;
		public static final int CATEGORY_BUTTON_HEIGHT = 20;
		
	}
	
	
	
	//==============//
	// GUI handling //
	//==============//
	
	/** if you want to get this config gui's screen call this */
	public static Screen getScreen(Screen parent, String category)
	{ return new DhConfigScreen(parent, category); }
	
	
	
	//================//
	// helper classes //
	//================//
	
	public static class ConfigListWidget extends ContainerObjectSelectionList<DhButtonEntry>
	{
		Font textRenderer;
		
		public ConfigListWidget(Minecraft minecraftClient, int canvasWidth, int canvasHeight, int topMargin, int botMargin, int itemSpacing)
		{
			#if MC_VER < MC_1_20_4
			super(minecraftClient, canvasWidth, canvasHeight, topMargin, canvasHeight - botMargin, itemSpacing);
			#else
			super(minecraftClient, canvasWidth, canvasHeight - (topMargin + botMargin), topMargin, itemSpacing);
			#endif
			
			this.centerListVertically = false;
			this.textRenderer = minecraftClient.font;
		}
		
		public void addButton(DhConfigScreen gui, AbstractConfigBase dhConfigType, AbstractWidget button, AbstractWidget resetButton, AbstractWidget indexButton, Component text)
		{ this.addEntry(new DhButtonEntry(gui, dhConfigType, button, text, resetButton, indexButton)); }
		
		@Override
		public int getRowWidth() { return 10_000; }
		
		public AbstractWidget getHoveredButton(double mouseX, double mouseY)
		{
			for (DhButtonEntry buttonEntry : this.children())
			{
				AbstractWidget button = buttonEntry.button;
				if (button != null 
					&& button.visible)
				{
					#if MC_VER < MC_1_19_4
					double minX = button.x;
					double minY = button.y;
					#else
					double minX = button.getX();
					double minY = button.getY();
					#endif
					
					double maxX = minX + button.getWidth();
					double maxY = minY + button.getHeight();
					
					if (mouseX >= minX && mouseX < maxX
						&& mouseY >= minY && mouseY < maxY)
					{
						return button;
					}
				}
			}
			
			return null;
		}
		
	}
	
	
	public static class DhButtonEntry extends ContainerObjectSelectionList.Entry<DhButtonEntry>
	{
		private static final Font textRenderer = Minecraft.getInstance().font;
		
		private final AbstractWidget button;
		
		private final DhConfigScreen gui;
		
		private final AbstractWidget resetButton;
		private final AbstractWidget indexButton;
		private final Component text;
		private final List<AbstractWidget> children = new ArrayList<>();
		
		@NotNull
		private final EConfigCommentTextPosition textPosition;
		public final AbstractConfigBase dhConfigType;
		
		public static final Map<AbstractWidget, Component> TEXT_BY_WIDGET = new HashMap<>();
		public static final Map<AbstractWidget, DhButtonEntry> BUTTON_BY_WIDGET = new HashMap<>();
		
		
		
		public DhButtonEntry(
			DhConfigScreen gui, AbstractConfigBase dhConfigType, 
				AbstractWidget button, Component text, AbstractWidget resetButton, AbstractWidget indexButton)
		{
			TEXT_BY_WIDGET.put(button, text);
			BUTTON_BY_WIDGET.put(button, this);
			
			this.gui = gui;
			this.dhConfigType = dhConfigType;
			
			this.button = button;
			this.resetButton = resetButton;
			this.text = text;
			this.indexButton = indexButton;
			
			if (button != null) { this.children.add(button); }
			if (resetButton != null) { this.children.add(resetButton); }
			if (indexButton != null) { this.children.add(indexButton); }
			
			
			EConfigCommentTextPosition textPosition = null;
			if (this.dhConfigType instanceof ConfigUIComment)
			{
				textPosition = ((ConfigUIComment)this.dhConfigType).textPosition;
			}
			
			if (textPosition == null)
			{
				if (this.button != null)
				{
					// if a button is present
					textPosition = EConfigCommentTextPosition.RIGHT_JUSTIFIED;
				}
				else
				{
					textPosition = EConfigCommentTextPosition.CENTERED_OVER_BUTTONS;
				}
			}
			this.textPosition = textPosition;
			
		}
		
		
		
		@Override
        #if MC_VER < MC_1_20_1
		public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
        #elif MC_VER < MC_1_21_9
		public void render(GuiGraphics matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
		#elif MC_VER <= MC_1_21_11
		public void renderContent(GuiGraphics matrices, int mouseX, int mouseY, boolean hovered, float tickDelta)
		#else
		public void extractContent(GuiGraphicsExtractor matrices, int mouseX, int mouseY, boolean hovered, float tickDelta)
		#endif
		{
			try
			{
				// setting the "y" variable is necessary so each child item
				// renders at the correct height,
				// if not set they will render off-screen.
				#if MC_VER < MC_1_21_9
				// Y value passed in from method args
				#else
				int y = this.getY();
				#endif
				
				
				
				if (this.button != null)
				{
					SetY(this.button, y);
					#if MC_VER <= MC_1_21_11
					this.button.render(matrices, mouseX, mouseY, tickDelta);
					#else
					this.button.extractRenderState(matrices, mouseX, mouseY, tickDelta);
					#endif
				}
				
				if (this.resetButton != null)
				{
					SetY(this.resetButton, y);
					#if MC_VER <= MC_1_21_11
					this.resetButton.render(matrices, mouseX, mouseY, tickDelta);
					#else
					this.resetButton.extractRenderState(matrices, mouseX, mouseY, tickDelta);
					#endif
				}
				
				if (this.indexButton != null)
				{
					SetY(this.indexButton, y);
					#if MC_VER <= MC_1_21_11
					this.indexButton.render(matrices, mouseX, mouseY, tickDelta);
					#else
					this.indexButton.extractRenderState(matrices, mouseX, mouseY, tickDelta);
					#endif
				}
				
				if (this.text != null)
				{
					int translatedLength = textRenderer.width(this.text);
					
					int textXPos;
					if (this.textPosition == EConfigCommentTextPosition.RIGHT_JUSTIFIED)
					{
						// text right justified aligned against the buttons
						textXPos = this.gui.width
								- translatedLength
								- ConfigScreenConfigs.SPACE_BETWEEN_TEXT_AND_OPTION_FIELD
								- ConfigScreenConfigs.SPACE_FROM_RIGHT_SCREEN
								- ConfigScreenConfigs.OPTION_FIELD_WIDTH
								- ConfigScreenConfigs.BUTTON_WIDTH_SPACING
								- ConfigScreenConfigs.RESET_BUTTON_WIDTH;
					}
					else if (this.textPosition == EConfigCommentTextPosition.CENTERED_OVER_BUTTONS)
					{
						// have button centered relative to a category button
						textXPos = this.gui.width
								- (translatedLength / 2)
								- (ConfigScreenConfigs.CATEGORY_BUTTON_WIDTH / 2)
								- ConfigScreenConfigs.SPACE_FROM_RIGHT_SCREEN;
					}
					else if (this.textPosition == EConfigCommentTextPosition.CENTER_OF_SCREEN)
					{
						// have button centered in the screen
						textXPos = (this.gui.width / 2)
								- (translatedLength / 2);
					}
					else
					{
						throw new UnsupportedOperationException("No text position render defined for [" + this.textPosition + "]");
					}
				
				
                #if MC_VER < MC_1_20_1
				GuiComponent.drawString(matrices, textRenderer, 
					this.text, 
					textXPos, y + 5, 
					0xFFFFFF);
				#elif MC_VER < MC_1_21_6
					matrices.drawString(textRenderer,
							this.text,
							textXPos, y + 5,
							0xFFFFFF);
				#elif MC_VER <= MC_1_21_11
				matrices.drawString(textRenderer, 
						this.text,
						textXPos, y + 5, 
						0xFFFFFFFF);
				#else
				matrices.text(textRenderer, 
						this.text,
						textXPos, y + 5, 
						0xFFFFFFFF);
				#endif
				}
			}
			catch (Exception e)
			{
				// should prevent crashing the game if there's an issue
				RATE_LIMITED_LOGGER.error("Unexpected gui rendering issue: ["+e.getMessage()+"]", e);
			}
		}
		
		@Override
		public @NotNull List<? extends GuiEventListener> children()
		{ return this.children; }
		
		#if MC_VER >= MC_1_17_1
		@Override
		public @NotNull List<? extends NarratableEntry> narratables()
		{ return this.children; }
		#endif
		
		
		
	}
	
	
	
	//================//
	// event handling //
	//================//
	
	public static class ConfigCoreInterface implements IConfigGui
	{
		/**
		 * in the future it would be good to pass in the current page and other variables, 
		 * but for now just knowing when the page is closed is good enough 
		 */
		public final ArrayList<Runnable> onScreenChangeListenerList = new ArrayList<>();
		
		
		
		@Override
		public void addOnScreenChangeListener(Runnable newListener) { this.onScreenChangeListenerList.add(newListener); }
		@Override
		public void removeOnScreenChangeListener(Runnable oldListener) { this.onScreenChangeListenerList.remove(oldListener); }
		
	}
	
}

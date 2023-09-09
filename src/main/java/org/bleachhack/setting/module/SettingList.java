/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.setting.module;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;

import net.minecraft.text.Text;

import org.bleachhack.gui.clickgui.window.ModuleWindow;
import org.bleachhack.gui.window.Window;
import org.bleachhack.gui.window.WindowScreen;
import org.bleachhack.gui.window.widget.WindowButtonWidget;
import org.bleachhack.gui.window.widget.WindowScrollbarWidget;
import org.bleachhack.gui.window.widget.WindowTextFieldWidget;
import org.bleachhack.setting.SettingDataHandler;
import org.bleachhack.setting.SettingDataHandlers;
import org.bleachhack.util.io.BleachFileHelper;

import java.util.*;

public abstract class SettingList<T> extends ModuleSetting<LinkedHashSet<T>> {

	protected String windowText;
	protected Set<T> itemPool;

	@SuppressWarnings("unchecked")
	public SettingList(String text, String windowText, SettingDataHandler<T> itemHandler, Collection<T> itemPool, T... defaultItems) {
		super(text, new LinkedHashSet<>(Arrays.asList(defaultItems)), v -> (LinkedHashSet<T>) v.clone(), SettingDataHandlers.ofCollection(itemHandler, LinkedHashSet::new));
		this.windowText = windowText;
		this.itemPool = new LinkedHashSet<>(itemPool);
	}

	public void render(ModuleWindow window, DrawContext context, int x, int y, int len) {
		if (window.mouseOver(x, y, x + len, y + 12)) {
			context.fill(x + 1, y, x + len, y + 12, 0x70303070);
		}

		context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getName(), x + 3, y + 2, 0xcfe0cf);

		context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, "...", x + len - 7, y + 2, 0xcfd0cf);

		if (window.mouseOver(x, y, x + len, y + 12) && window.lmDown) {
			window.mouseReleased(window.mouseX, window.mouseY, 1);
			MinecraftClient.getInstance().currentScreen.mouseReleased(window.mouseX, window.mouseY, 0);
			MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.3F));
			MinecraftClient.getInstance().setScreen(new ListWidowScreen(MinecraftClient.getInstance().currentScreen));
		}
	}

	public boolean contains(T item) {
		return getValue().contains(item);
	}

	public void renderItem(MinecraftClient mc, DrawContext context, T item, int x, int y, int w, int h) {
		context.getMatrices().push();

		float scale = (h - 2) / 10f;
		float offset = 1f / scale;

		context.getMatrices().scale(scale, scale, 1f);

		context.drawTextWithShadow(mc.textRenderer, "?", (int) ((x + 5) * offset), (int) ((y + 4) * offset), -1);

		context.getMatrices().pop();
	}

	/**
	 * The human readable name for this item, the internal name is used for read/writing.
	 */
	public abstract Text getName(T item);

	public SettingList<T> withDesc(String desc) {
		setTooltip(desc);
		return this;
	}

	public int getHeight(int len) {
		return 12;
	}

	private class ListWidowScreen extends WindowScreen {

		private Screen parent;
		private WindowTextFieldWidget inputField;
		private WindowScrollbarWidget scrollbar;

		private T toDeleteItem;
		private T toAddItem;

		public ListWidowScreen(Screen parent) {
			super(Text.literal(windowText));
			this.parent = parent;
		}

		public void init() {
			super.init();

			clearWindows();

			addWindow(new Window(
					(int) (width / 3.25),
					height / 12,
					(int) (width - width / 3.25),
					height - height / 12,
					windowText, new ItemStack(Items.OAK_SIGN)));

			int x2 = getWindow(0).x2 - getWindow(0).x1;
			int y2 = getWindow(0).y2 - getWindow(0).y1;

			getWindow(0).addWidget(new WindowButtonWidget(x2 - 50, y2 - 22, x2 - 5, y2 - 5, "Reset", () -> {
				getValue().clear();
				getValue().addAll(defaultValue);
				BleachFileHelper.SCHEDULE_SAVE_MODULES.set(true);
			}));

			getWindow(0).addWidget(new WindowButtonWidget(x2 - 100, y2 - 22, x2 - 55, y2 - 5, "Clear", () -> {
				getValue().clear();
				BleachFileHelper.SCHEDULE_SAVE_MODULES.set(true);
			}));

			getWindow(0).addWidget(new WindowButtonWidget(x2 - 150, y2 - 22, x2 - 105, y2 - 5, "Add All", () -> {
				getValue().clear();
				getValue().addAll(itemPool);
				BleachFileHelper.SCHEDULE_SAVE_MODULES.set(true);
			}));

			inputField = getWindow(0).addWidget(new WindowTextFieldWidget(5, y2 - 22, x2 / 3, 17, inputField != null ? inputField.textField.getText() : ""));

			scrollbar = getWindow(0).addWidget(new WindowScrollbarWidget(x2 - 11, 12, 0, y2 - 39, scrollbar == null ? 0 : scrollbar.getPageOffset()));
		}

		public void render(DrawContext context, int mouseX, int mouseY, float delta) {
			renderBackground(context);
			super.render(context, mouseX, mouseY, delta);
		}

		public void onRenderWindow(DrawContext context, int window, int mouseX, int mouseY) {
			super.onRenderWindow(context, window, mouseX, mouseY);

			toAddItem = null;
			toDeleteItem = null;

			if (window == 0) {
				int x1 = getWindow(0).x1;
				int y1 = getWindow(0).y1;
				int x2 = getWindow(0).x2;
				int y2 = getWindow(0).y2;

				int maxEntries = Math.max(1, (y2 - y1) / 21 - 1);
				int renderEntries = 0;
				int entries = 0;

				scrollbar.setTotalHeight(getValue().size() * 21);
				int offset = scrollbar.getPageOffset();

				for (T e: getValue()) {
					if (entries >= offset / 21 && renderEntries < maxEntries) {
						drawEntry(context, e, x1 + 6, y1 + 15 + entries * 21 - offset, x2 - x1 - 19, 20, mouseX, mouseY);
						renderEntries++;
					}

					entries++;
				}

				//Window.horizontalGradient(matrix, x1 + 1, y2 - 25, x2 - 1, y2 - 1, 0x70606090, 0x00606090);
				Window.horizontalGradient(context, x1 + 1, y2 - 27, x2 - 1, y2 - 26, 0xff606090, 0x50606090);

				if (inputField.textField.isFocused()) {
					Set<T> toDraw = new LinkedHashSet<>();

					for (T e: itemPool) {
						if (toDraw.size() >= 10)
							break;

						if (!getValue().contains(e) && getName(e).getString().toLowerCase(Locale.ENGLISH).contains(inputField.textField.getText().toLowerCase(Locale.ENGLISH))) {
							toDraw.add(e);
						}
					}

					int curY = y1 + inputField.y1 - 4 - toDraw.size() * 17;
					int longest = toDraw.stream().mapToInt(e -> textRenderer.getWidth(getName(e))).max().orElse(0);

					RenderSystem.getModelViewStack().push();
					RenderSystem.getModelViewStack().translate(0, 0, 150);

					context.getMatrices().push();
					context.getMatrices().translate(0, 0, 150);

					for (T e: toDraw) {
						drawSearchEntry(context, e, x1 + inputField.x1, curY, longest + 23, 16, mouseX, mouseY);
						curY += 17;
					}

					context.getMatrices().pop();
					RenderSystem.getModelViewStack().pop();
					RenderSystem.applyModelViewMatrix();
				}
			}
		}

		private void drawEntry(DrawContext context, T item, int x, int y, int width, int height, int mouseX, int mouseY) {
			boolean mouseOverDelete = mouseX >= x + width - 14 && mouseX <= x + width - 1 && mouseY >= y + 2 && mouseY <= y + height - 2;
			Window.fill(context, x + width - 14, y + 2, x + width - 1, y + height - 2, mouseOverDelete ? 0x4fb070f0 : 0x60606090);

			if (mouseOverDelete) {
				toDeleteItem = item;
			}

			renderItem(client, context, item, x, y, height, height);

			context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getName(item), x + height + 4, y + 4, -1);
			context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, "\u00a7cx", x + width - 10, y + 5, -1);
		}

		private void drawSearchEntry(DrawContext context, T item, int x, int y, int width, int height, int mouseX, int mouseY) {
			boolean mouseOver = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
			context.fill(x, y - 1, x + width, y + height, mouseOver ? 0xdf8070d0 : 0xb0606090);

			if (mouseOver) {
				toAddItem = item;
			}

			renderItem(client, context, item, x, y, height, height);

			context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getName(item), x + height + 4, y + 4, -1);
		}

		@Override
		public void close() {
			this.client.setScreen(parent);
		}

		@Override
		public boolean shouldPause() {
			return false;
		}

		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (toAddItem != null) {
				getValue().add(toAddItem);
				inputField.textField.setFocused(true);
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.3F));
				BleachFileHelper.SCHEDULE_SAVE_MODULES.set(true);
				return false;
			} else if (toDeleteItem != null) {
				getValue().remove(toDeleteItem);
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.3F));
				BleachFileHelper.SCHEDULE_SAVE_MODULES.set(true);
			}

			return super.mouseClicked(mouseX, mouseY, button);
		}

		public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
			if (!inputField.textField.isFocused() || inputField.textField.getText().isEmpty()) {
				scrollbar.scroll(amount);
			}

			return super.mouseScrolled(mouseX, mouseY, amount);
		}
	}
}

/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.event.events;

import java.util.List;

import net.minecraft.client.gui.DrawContext;
import org.bleachhack.event.Event;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.math.MatrixStack;

public class EventRenderTooltip extends Event {

	private Screen screen;
	private DrawContext context;
	private int x;
	private int y;
	private int mouseX;
	private int mouseY;
	private float delta;

	public EventRenderTooltip(Screen screen, DrawContext context, int mouseX, int mouseY, float delta) {
		this.context = context;
		this.screen = screen;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.delta = delta;
	}
	
	public Screen getScreen() {
		return screen;
	}

	public DrawContext getContext() {
		return context;
	}

	public void setContext(DrawContext context) {
		this.context = context;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getMouseX() {
		return mouseX;
	}

	public int getMouseY() {
		return mouseY;
	}

	public float getDelta() {
		return delta;
	}
	
}

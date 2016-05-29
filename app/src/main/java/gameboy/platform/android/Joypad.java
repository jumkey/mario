/**
 * Mario GameBoy (TM) Emulator
 * 
 * J2ME MIDP 2.0 Joypad Input Driver
 *
 * Copyright (C) 2006  Carlos Hasan.  All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package gameboy.platform.android;


import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import org.cafeboy.mario.GameView;

import gameboy.core.driver.JoypadDriver;

public class Joypad implements JoypadDriver,View.OnKeyListener,View.OnTouchListener {

	/*
	 * Virtual Keys
	 */
	private static final int VK_DOWN	= KeyEvent.KEYCODE_DPAD_DOWN;
	private static final int VK_UP		= KeyEvent.KEYCODE_DPAD_UP;
	private static final int VK_LEFT	= KeyEvent.KEYCODE_DPAD_LEFT;
	private static final int VK_RIGHT	= KeyEvent.KEYCODE_DPAD_RIGHT;

	private static final int VK_SELECT	= KeyEvent.KEYCODE_ENTER;
	private static final int VK_START	= KeyEvent.KEYCODE_SPACE;

	private static final int VK_BUTTON_B = KeyEvent.KEYCODE_B;
	private static final int VK_BUTTON_A = KeyEvent.KEYCODE_A;



	private int buttons;
	private int directions;
	private boolean raised;

	public Joypad()
	{
		buttons = 0;
		directions = 0;
		raised = false;
	}

	public final boolean isRaised()
	{
		boolean result = raised;
		raised = false;
		return result;
	}

	public final int getButtons()
	{
		return buttons ^ 0x0F;
	}

	public final int getDirections()
	{
		return directions ^ 0x0F;
	}


	public final void keyPressed(int keyCode)
	{
		switch (keyCode) {
			case VK_DOWN:
				directions |= BUTTON_DOWN;
				directions &= ~BUTTON_UP;
				break;
			case VK_UP:
				directions |= BUTTON_UP;
				directions &= ~BUTTON_DOWN;
				break;
			case VK_LEFT:
				directions |= BUTTON_LEFT;
				directions &= ~BUTTON_RIGHT;
				break;
			case VK_RIGHT:
				directions |= BUTTON_RIGHT;
				directions &= ~BUTTON_LEFT;
				break;

			case VK_START:
				buttons |= BUTTON_START;
				break;
			case VK_SELECT:
				buttons |= BUTTON_SELECT;
				break;
			case VK_BUTTON_B:
				buttons |= BUTTON_B;
				break;
			case VK_BUTTON_A:
				buttons |= BUTTON_A;
				break;
		}

		raised = true;
	}

	public final void keyReleased(int keyCode)
	{
		switch (keyCode) {
			case VK_DOWN:
				directions &= ~BUTTON_DOWN;
				break;
			case VK_UP:
				directions &= ~BUTTON_UP;
				break;
			case VK_LEFT:
				directions &= ~BUTTON_LEFT;
				break;
			case VK_RIGHT:
				directions &= ~BUTTON_RIGHT;
				break;

			case VK_START:
				buttons &= ~BUTTON_START;
				break;
			case VK_SELECT:
				buttons &= ~BUTTON_SELECT;
				break;
			case VK_BUTTON_B:
				buttons &= ~BUTTON_B;
				break;
			case VK_BUTTON_A:
				buttons &= ~BUTTON_A;
				break;
		}

		raised = true;
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		switch (event.getAction()) {
			case KeyEvent.ACTION_DOWN:          //键盘按下
				keyPressed(keyCode);
				break;
			case KeyEvent.ACTION_UP:             //键盘松开
				keyReleased(keyCode);
				break;
		}
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		GameView s = (GameView) v;
		// check whether a button was pressed or released
		if(s.getButtons() != null) {
			for(int i = 0 ; i < s.getButtons().length ; ++i) {
				// the event belongs to this button?
				if(s.getButtons()[i].getBounds().contains((int)event.getX(), (int)event.getY())) {
					// simulate a key event that corresponds to the button
					if(event.getAction() == MotionEvent.ACTION_DOWN) {
						keyPressed(s.getButtonKeys()[i]);
					} else if(event.getAction() == MotionEvent.ACTION_UP) {
						keyReleased(s.getButtonKeys()[i]);
					}
				}
			}
		}
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:          //屏幕按下
			keyPressed(VK_BUTTON_A);
			break;
		case MotionEvent.ACTION_UP:             //屏幕松开
			keyReleased(VK_BUTTON_A);
			keyReleased(VK_BUTTON_B);
			keyReleased(VK_START);
			keyReleased(VK_SELECT);
			keyReleased(VK_DOWN);
			keyReleased(VK_UP);
			keyReleased(VK_RIGHT);
			keyReleased(VK_LEFT);
			break;
		case MotionEvent.ACTION_MOVE:
			if(event.getHistorySize() > 0) {
				final int hist = event.getHistorySize();
				final float xmove = event.getX() - event.getHistoricalX(hist - 1);
				final float ymove = event.getY() - event.getHistoricalY(hist - 1);

				// horizontal movement?
				if (Math.abs(xmove) > Math.abs(ymove)) {
					if (xmove < 0) {
						keyPressed(VK_LEFT);
					} else {
						keyPressed(VK_RIGHT);
					}
					// no, vertical movement
				} else {
					if (ymove < 0) {
						keyPressed(VK_UP);
					} else {
						keyPressed(VK_DOWN);
					}
				}
			}
	}
		return false;
	}
}

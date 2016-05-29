/**
 * Mario GameBoy (TM) Emulator
 * 
 * Joypad Input Driver
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
package gameboy.core.driver;

public interface JoypadDriver {	
	public static final int BUTTON_DOWN		= 0x08;
	public static final int BUTTON_UP		= 0x04;
	public static final int BUTTON_LEFT		= 0x02;
	public static final int BUTTON_RIGHT	= 0x01;

	public static final int BUTTON_START	= 0x08;
	public static final int BUTTON_SELECT	= 0x04;
	public static final int BUTTON_B		= 0x02;
	public static final int BUTTON_A		= 0x01;
	
	public boolean isRaised();
	
	public int getButtons();

	public int getDirections();
}

/**
 * Mario GameBoy (TM) Emulator
 * 
 * Joypad Input
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
package gameboy.core;

import gameboy.core.driver.JoypadDriver;

public class Joypad {
	/*
	 * Joypad Registers
	 */
	public static final int JOYP = 0xFF00;		/* P1 */
	
	
	/*
	 * Gameboy Clock Speed (1048576 Hz)
	 */
	private static final int GAMEBOY_CLOCK = 1 << 20;

	/*
	 * Joypad Poll Speed (64 Hz)
	 */
	private static final int JOYPAD_CLOCK = GAMEBOY_CLOCK >> 6;
	
	/*
	 * Registers
	 */
	private int joyp;
	private int cycles;
	
	/*
	 * Interrupt Controller
	 */
	private Interrupt interrupt;
	
	/*
	 * Driver
	 */
	private JoypadDriver driver;
	
	public Joypad(JoypadDriver driver, Interrupt interrupt)
	{
		this.driver = driver;
		this.interrupt = interrupt;
		
		reset();
	}
	
	public final void reset()
	{
		joyp = 0xFF;
		cycles = JOYPAD_CLOCK;
	}

	public int cycles()
	{
		return cycles;
	}
	
	public final void emulate(int ticks)
	{
		cycles -= ticks;

		if (cycles <= 0) {
			if (driver.isRaised())
				update();
			
			cycles = JOYPAD_CLOCK;
		}
	}
	
	public final void write(int address, int data)
	{
		if (address == JOYP) {
			joyp = (joyp & 0xCF) + (data & 0x30);			
			update();
		}
	}
	
	public final int read(int address)
	{
		if (address == JOYP)
			return joyp;
		
		return 0xFF;
	}

	private final void update()
	{
		int data = joyp & 0xF0;
		
		switch (data & 0x30) {
		case 0x10:
			data |= driver.getButtons();
			break;
		case 0x20:
			data |= driver.getDirections();
			break;
		case 0x30:
			data |= 0x0F;
		}
		
		if ((joyp & ~data & 0x0F) != 0)
			interrupt.raise(Interrupt.JOYPAD);
		
		joyp = data;
	}
	
	public final String toString()
	{
		return "JOYP=" + Integer.toHexString(joyp);
	}
}

/**
 * Mario GameBoy (TM) Emulator
 * 
 * Interrupt Controller
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

public class Interrupt {
	/*
	 * Interrupt Registers
	 */
	public static final int IE		= 0xFFFF;		/* Interrupt Enable */
	public static final int IF		= 0xFF0F;		/* Interrupt Flag */

	/*
	 * Interrupt Flags
	 */
	public static final int VBLANK	= 0x01;			/* V-Blank Interrupt  (INT 40h) */
	public static final int LCD		= 0x02;			/* LCD STAT Interrupt (INT 48h) */
	public static final int TIMER	= 0x04;			/* Timer Interrupt    (INT 50h) */
	public static final int SERIAL 	= 0x08;			/* Serial Interrupt   (INT 58h) */
	public static final int JOYPAD	= 0x10;			/* Joypad Interrupt   (INT 60h) */
	
	/*
	 * Registers
	 */
	private int enable;
	private int flag;
	
	public Interrupt()
	{
		reset();
	}

	public final void reset()
	{
		enable = 0;
		flag = VBLANK;
	}
	
	public final boolean isPending()
	{
		return (enable & flag) != 0;
	}
	
	public final boolean isPending(int mask)
	{
		return (enable & flag & mask) != 0;			
	}

	public final void raise(int mask)
	{
		flag |= mask;
	}
	
	public final void lower(int mask)
	{
		flag &= ~mask;
	}
	
	public final void write(int address, int data)
	{		
		switch (address) {
		case IE:
			setInterruptEnable(data);
			break;
		case IF:
			setInterruptFlag(data);
			break;
		}
	}

	public final int read(int address)
	{
		switch (address) {
		case IE:
			return getInterruptEnable();
		case IF:
			return getInterruptFlag();
		}		
		return 0xFF;
	}

	private final int getInterruptEnable()
	{
		return enable;
	}
	
	private final int getInterruptFlag()
	{
		return 0xE0 | flag;
	}

	private final void setInterruptEnable(int data)
	{
		enable = data;
	}
	
	private final void setInterruptFlag(int data)
	{
		flag = data;
	}
	
	public final String toString()
	{
		return "IE=" + Integer.toHexString(enable) + " IF=" + Integer.toHexString(flag);
	}
}

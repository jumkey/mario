/**
 * Mario GameBoy (TM) Emulator
 * 
 * Serial Link Controller
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

public class Serial {
	/*
	 * Gameboy Clock Speed (1048576 Hz)
	 */
	private static final int GAMEBOY_CLOCK = 1 << 20;
	
	/*
	 * Serial Clock Speed (8 x 1024 bits/sec)
	 */
	private static final int SERIAL_CLOCK = GAMEBOY_CLOCK >> 16;
	
	/*
	 * Serial Idle Speed (128 Hz)
	 */
	private static final int SERIAL_IDLE_CLOCK = GAMEBOY_CLOCK >> 7;
	
	/*
	 * Serial Register Addresses
	 */
	public static final int SB	= 0xFF01;		/* Serial Transfer Data */
	public static final int SC	= 0xFF02;		/* Serial Transfer Control */

	/*
	 * Registers
	 */
	private int sb;
	private int sc;
	private int cycles;

	/*
	 * Interrupt Controller
	 */
	private Interrupt interrupt;	
	
	public Serial(Interrupt interrupt)
	{
		this.interrupt = interrupt;
		
		reset();
	}
	
	public final void reset()
	{
		cycles = SERIAL_CLOCK;
		sb = 0x00;
		sc = 0x00;
	}

	public final int cycles()
	{
		return cycles;
	}

	public final void emulate(int ticks)
	{
		if ((sc & 0x81) == 0x81) {
			cycles -= ticks;
		
			if (cycles <= 0) {
				sb = 0xFF;
				sc &= 0x7F;
				cycles = SERIAL_IDLE_CLOCK;
				
				interrupt.raise(Interrupt.SERIAL);
			}			
		}
	}
	
	public final void write(int address, int data)
	{
		if (address == SB) {
			setSerialData(data);
		}
		else if (address == SC) {
			setSerialControl(data);
		}
	}

	public final int read(int address)
	{
		if (address == SB) {
			return getSerialData();
		}
		else if (address == SC) {
			return getSerialControl();
		}		
		return 0xFF;
	}

	private final void setSerialData(int data)
	{
		sb = data;
	}
	
	private final void setSerialControl(int data)
	{
		sc = data;
		
		// HACK: delay the serial interrupt (Shin Nihon Pro Wrestling)
		cycles = SERIAL_IDLE_CLOCK + SERIAL_CLOCK;
	}

	private final int getSerialData()
	{
		return sb;
	}

	private final int getSerialControl()
	{
		return sc;
	}
	
	public final String toString()
	{
		return "SB=" + Integer.toHexString(sb) + " SC=" + Integer.toHexString(sc);
	}
}

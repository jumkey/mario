/**
 * Mario GameBoy (TM) Emulator
 * 
 * Timer and Divider
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

public class Timer {
	/*
	 * Gameboy Clock Speed (1048576 Hz)
	 */
	private static final int GAMEBOY_CLOCK = 1 << 20;
	
	/*
	 * DIV Timer Speed (16384 Hz)
	 */
	private static final int DIV_CLOCK = GAMEBOY_CLOCK >> 14;

	/*
	 * Timer Clock Speeds (4096, 262144, 65536 and 16384 Hz)
	 */
	private static final int TIMER_CLOCK[] = {
		GAMEBOY_CLOCK >> 12,
		GAMEBOY_CLOCK >> 18,
		GAMEBOY_CLOCK >> 16,
		GAMEBOY_CLOCK >> 14
	}; 
	
	/*
	 * Timer Register Addresses
	 */
	public static final int DIV		= 0xFF04;	/* Divider Register */
	public static final int TIMA	= 0xFF05;	/* Timer Counter */
	public static final int TMA		= 0xFF06;	/* Timer Modulo */
	public static final int TAC		= 0xFF07;	/* Timer Control */
	
	/*
	 * Registers
	 */
	private int div;
	private int tima;
	private int tma;
	private int tac;
	
	private int dividerCycles;
	private int timerCycles;
	private int timerClock;

	/*
	 * Interrupt Controller
	 */
	private Interrupt interrupt;
	
	public Timer(Interrupt interrupt)
	{
		this.interrupt = interrupt;
		
		reset();
	}

	public final void reset()
	{
		div = 0;
		dividerCycles = DIV_CLOCK;
		tima = tma = tac = 0x00;
		timerCycles = timerClock = TIMER_CLOCK[tac & 0x03];
	}
	
	public final void write(int address, int data)
	{
		switch (address) {
		case DIV:
			setDivider(data);
			break;
		case TIMA:
			setTimerCounter(data);
			break;
		case TMA:
			setTimerModulo(data);
			break;
		case TAC:
			setTimerControl(data);
			break;
		}
	}

	public final int read(int address)
	{
		switch (address) {
		case DIV:
			return getDivider();
		case TIMA:
			return getTimerCounter();
		case TMA:
			return getTimerModulo();
		case TAC:
			return getTimerControl();
		}
		return 0xFF;
	}

	private final void setDivider(int data)
	{
		// DIV register resets on write
		div = 0;
	}

	private final void setTimerCounter(int data)
	{
		tima = data;
	}

	private final void setTimerModulo(int data)
	{
		tma = data;
	}

	private final void setTimerControl(int data)
	{
		if ((tac & 0x03) != (data & 0x03))
			timerCycles = timerClock = TIMER_CLOCK[data & 0x03];
		
		tac = data;
	}

	private final int getDivider()
	{
		return div;
	}

	private final int getTimerCounter()
	{
		return tima;
	}

	private final int getTimerModulo()
	{
		return tma;
	}

	private final int getTimerControl()
	{
		return 0xF8 | tac;
	}
	
	public final int cycles()
	{
		if ((tac & 0x04) != 0) {
			if (timerCycles < dividerCycles)
				return timerCycles;
		}
		return dividerCycles;
	}

	public final void emulate(int ticks)
	{
		emulateDivider(ticks);
		emulateTimer(ticks);
	}
	
	private final void emulateDivider(int ticks)
	{
		dividerCycles -= ticks;
		
		while (dividerCycles <= 0) {
			div = (div + 1) & 0xFF;
			dividerCycles += DIV_CLOCK;
		}
	}
	
	private final void emulateTimer(int ticks)
	{
		if ((tac & 0x04) != 0) {
			timerCycles -= ticks;

			while (timerCycles <= 0) {
				tima = (tima + 1) & 0xFF;
				timerCycles += timerClock;
				
				if (tima == 0x00) {
					tima = tma;
					
					interrupt.raise(Interrupt.TIMER);
				}
			}
		}
	}
	
	public final String toString()
	{
		return "DIV=" + Integer.toHexString(div) + " TIMA=" + Integer.toHexString(tima) + " TMA=" + Integer.toHexString(tma) + " TAC=" + Integer.toHexString(tac);
	}
}

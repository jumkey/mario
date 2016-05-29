/**
 * Mario GameBoy (TM) Emulator
 * 
 * Work and High RAM
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

public class RAM {
	/*
	 * Work RAM
	 */
	private byte[] wram = new byte[8192];
	
	/*
	 * High RAM
	 */
	private byte[] hram = new byte[128];
	
	public RAM()
	{
		reset();
	}
	
	public final void reset()
	{
		for (int index = 0; index < wram.length; index++)
			wram[index] = (byte) 0x00;
		
		for (int index = 0; index < hram.length; index++)
			hram[index] = (byte) 0x00;
	}
	
	public final void write(int address, int data)
	{
		if (address >= 0xC000 && address <= 0xFDFF) {
			// C000-DFFF Work RAM (8KB)
			// E000-FDFF Echo RAM
			wram[address & 0x1FFF] = (byte) data;
		}
		else if (address >= 0xFF80 && address <= 0xFFFE) {
			// FF80-FFFE High RAM
			hram[address & 0x7F] = (byte) data;
		}
	}

	public final int read(int address)
	{
		if (address >= 0xC000 && address <= 0xFDFF) {
			// C000-DFFF Work RAM
			// E000-FDFF Echo RAM
			return wram[address & 0x1FFF] & 0xFF;
		}
		else if (address >= 0xFF80 && address <= 0xFFFE) {
			// FF80-FFFE High RAM
			return hram[address & 0x7F] & 0xFF;
		}		
		return 0xFF;
	}
}

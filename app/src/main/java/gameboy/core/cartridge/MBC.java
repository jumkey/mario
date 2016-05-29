/**
 * Mario GameBoy (TM) Emulator
 * 
 * Memory Bank Controller
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
package gameboy.core.cartridge;

public interface MBC {
	/*
	 * ROM Bank Size (16KB)
	 */
	public static final int ROM_BANK_SIZE = 0x4000;
	
	/*
	 * RAM Bank Size (8KB)
	 */
	public static final int RAM_BANK_SIZE = 0x2000;
	
	public void reset();
	
	public int read(int address);
	
	public void write(int address, int data);
}

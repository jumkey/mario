/**
 * Mario GameBoy (TM) Emulator
 * 
 * Hudson Memory Bank Controller 1 (2MB ROM, 32KB RAM)
 * 
 * 0000-3FFF	ROM Bank 0 (16KB)
 * 4000-7FFF	ROM Bank 1-127 (16KB)
 * A000-BFFF	RAM Bank 0-3 (8KB)
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

public class HuC1 extends MBC1 {
	public HuC1(byte[] rom, byte[] ram)
	{
		super(rom, ram);
	}
}

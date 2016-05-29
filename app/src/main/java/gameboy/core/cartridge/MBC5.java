/**
 * Mario GameBoy (TM) Emulator
 * 
 * Memory Bank Controller 5 (8MB ROM, 128KB RAM)
 *
 * 0000-3FFF	ROM Bank 0 (16KB)
 * 4000-7FFF	ROM Bank 1-511 (16KB)
 * A000-BFFF	RAM Bank 0-15 (8KB)
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

public class MBC5 implements MBC {
	private byte[] rom;
	private byte[] ram;

	private int romSize;
	private int ramSize;

	private int romBank;
	private int ramBank;
	
	private boolean ramEnable;
	private boolean rumble;

	public MBC5(byte[] rom, byte[] ram, boolean rumble)
	{
		this.rumble = rumble;
		
		setROM(rom);
		setRAM(ram);
	}

	public final void reset()
	{
		romBank = ROM_BANK_SIZE;
		ramBank = 0;

		ramEnable = false;
	}

	public final int read(int address)
	{
		if (address <= 0x3FFF) {
			// 0000-3FFF
			return rom[address] & 0xFF;
		}
		else if (address <= 0x7FFF) {
			// 4000-7FFF
			return rom[romBank + (address & 0x3FFF)] & 0xFF;
		}
		else if (address >= 0xA000 && address <= 0xBFFF) {
			// A000-BFFF
			return ram[ramBank + (address & 0x1FFF)] & 0xFF;
		}
		return 0xFF;
	}

	public final void write(int address, int data)
	{
		if (address <= 0x1FFF) {
			// 0000-1FFF
			if (ramSize > 0)
				ramEnable = ((data & 0x0A) == 0x0A);
		}
		else if (address <= 0x2FFF) {
			// 2000-2FFF
			romBank = ((romBank & (0x01 << 22)) + ((data & 0xFF) << 14)) & romSize;
		}
		else if (address <= 0x3FFF) {
			// 3000-3FFF
			romBank = ((romBank & (0xFF << 14)) + ((data & 0x01) << 22)) & romSize;
		}
		else if (address <= 0x4FFF) {
			// 4000-4FFF
			if (rumble)
				ramBank = ((data & 0x07) << 13) & ramSize;
			else
				ramBank = ((data & 0x0F) << 13) & ramSize;
		}
		else if (address >= 0xA000 && address <= 0xBFFF) {
			// A000-BFFF
			if (ramEnable)
				ram[ramBank + (address & 0x1FFF)] = (byte) data;
		}
	}
	
	private void setROM(byte[] buffer)
	{
		int banks = buffer.length / ROM_BANK_SIZE;

		if (banks < 2 || banks > 512)
			throw new RuntimeException("Invalid MBC5 ROM size");

		rom = buffer;
		romSize = ROM_BANK_SIZE * banks - 1;
	}

	private void setRAM(byte[] buffer)
	{
		int banks = buffer.length / RAM_BANK_SIZE;

		if (banks < 0 || banks > 16)
			throw new RuntimeException("Invalid MBC5 RAM size");

		ram = buffer;
		ramSize = RAM_BANK_SIZE * banks - 1;
	}
}

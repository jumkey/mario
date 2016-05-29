/**
 * Mario GameBoy (TM) Emulator
 * 
 * Memory Bank Controller 2 (256KB ROM, 512x4bit RAM)
 * 
 * 0000-3FFF	ROM Bank 0 (16KB)
 * 4000-7FFF	ROM Bank 1-15 (16KB)
 * A000-A1FF	RAM Bank (512x4bit)
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

public class MBC2 implements MBC {
	private static final int RAM_BANK_SIZE = 512;
	
	private byte[] rom;
	private byte[] ram;

	private int romSize;
	private int romBank;
	private boolean ramEnable;

	public MBC2(byte[] rom, byte[] ram)
	{
		setROM(rom);
		setRAM(ram);
	}

	public final void reset()
	{
		romBank = ROM_BANK_SIZE;

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
		else if (address >= 0xA000 && address <= 0xA1FF) {
			// A000-A1FF
			return ram[address & 0x01FF] & 0x0F;
		}
		return 0xFF;
	}

	public final void write(int address, int data)
	{
		if (address <= 0x1FFF) {
			// 0000-1FFF
			if ((address & 0x0100) == 0)
				ramEnable = ((data & 0x0A) == 0x0A);
		}
		else if (address <= 0x3FFF) {
			// 2000-3FFF
			if ((address & 0x0100) != 0) {
				if ((data & 0x0F) == 0)
					data = 1;
				romBank = ((data & 0x0F) << 14) & romSize;
			}
		}
		else if (address >= 0xA000 && address <= 0xA1FF) {
			// A000-A1FF
			if (ramEnable)
				ram[address & 0x01FF] = (byte) (data & 0x0F);
		}
	}
	
	private void setROM(byte[] buffer)
	{
		int banks = buffer.length / ROM_BANK_SIZE;

		if (banks < 2 || banks > 16)
			throw new RuntimeException("Invalid MBC2 ROM size");

		rom = buffer;
		romSize = ROM_BANK_SIZE * banks - 1;
	}

	private void setRAM(byte[] buffer)
	{
		if (buffer.length != RAM_BANK_SIZE)
			throw new RuntimeException("Invalid MBC2 RAM size");

		ram = buffer;
	}	
}

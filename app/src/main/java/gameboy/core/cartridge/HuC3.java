/**
 * Mario GameBoy (TM) Emulator
 * 
 * Hudson Memory Bank Controller 3 (2MB ROM, 128KB RAM, RTC)
 * 
 * 0000-3FFF	ROM Bank 0 (16KB)
 * 4000-7FFF	ROM Bank 1-127 (16KB)
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

import gameboy.core.driver.ClockDriver;

public class HuC3 implements MBC {
	private ClockDriver clock;
	private byte[] rom;
	private byte[] ram;
	private int romBank;
	private int ramBank;
	private int romSize;
	private int ramSize;
	private int ramFlag;
	private int ramValue;
	private int clockRegister;
	private int clockShift;
	private long clockTime;
	
	public HuC3(byte[] rom, byte[] ram, ClockDriver clock)
	{
		this.clock = clock;
		
		setROM(rom);
		setRAM(ram);
	}
	
	public final void reset()
	{
		romBank = ROM_BANK_SIZE;
		ramBank = 0;

		ramFlag = 0;
		ramValue = 0;
		
		clockRegister = 0;
		clockShift = 0;
		
		clockTime = clock.getTime();
	}

	public final int read(int address)
	{
		if (address <= 0x3FFF) {
			// 0000-3FFF
			return rom[address] & 0xFF;
		}
		else if (address <= 0x7FFF) {
			// 4000-5FFF
			return rom[romBank + (address & 0x3FFF)] & 0xFF;
		}
		else if (address >= 0xA000 && address <= 0xBFFF) {
			// A000-BFFF
			if (ramFlag == 0x0C) {
				return ramValue;
			}
			else if (ramFlag == 0x0D) {
				return 0x01;
			}
			else if (ramFlag == 0x0A || ramFlag == 0x00) {
				if (ramSize > 0)
					return ram[ramBank + (address & 0x1FFF)] & 0xFF;
			}
		}
		return 0xFF;
	}

	public final void write(int address, int data)
	{
		if (address <= 0x1FFF) {
			// 0000-1FFF
			ramFlag = data;
		}
		else if (address <= 0x3FFF) {
			// 2000-3FFF
			if ((data & 0x7F) == 0)
				data = 1;
			romBank = ((data & 0x7F) << 14) & romSize;
		}
		else if (address <= 0x5FFF) {
			// 4000-5FFF
			ramBank = ((data & 0x0F) << 13) & ramSize;
		}
		else if (address >= 0xA000 && address <= 0xBFFF) {
			// A000-BFFF
			if (ramFlag == 0x0B) {
				if ((data & 0xF0) == 0x10) {
					if (clockShift <= 24) {
						ramValue = (clockRegister >> clockShift) & 0x0F;
						clockShift += 4;
					}
				}
				else if ((data & 0xF0) == 0x30) {
					if (clockShift <= 24) {
						clockRegister &= ~(0x0F << clockShift);
						clockRegister |= ((data & 0x0F) << clockShift);
						clockShift += 4;
					}
				}
				else if ((data & 0xF0) == 0x40) {
					updateClock();						

					if ((data & 0x0F) == 0x00) {
						clockShift = 0;
					}
					else if ((data & 0x0F) == 0x03) {
						clockShift = 0;
					}
					else if ((data & 0x0F) == 0x07) {
						clockShift = 0;
					}
				}
				else if ((data & 0xF0) == 0x50) {

				}
				else if ((data & 0xF0) == 0x60) {
					ramValue = 0x01;
				}
			}
			else if (ramFlag >= 0x0C && ramFlag <= 0x0E) {

			}
			else if (ramFlag == 0x0A) {
				if (ramSize > 0)
					ram[ramBank + (address & 0x1FFF)] = (byte) data;
			}
		}
	}
	
	private final void updateClock()
	{
		long now = clock.getTime();
		
		long elapsed = now - clockTime;

		// years (4 bits)
		while (elapsed >= 365*24*60*60) {
			clockRegister += 1 << 24;
			elapsed -= 365*24*60*60;
		}
		
		// days (12 bits)
		while (elapsed >= 24*60*60) {
			clockRegister += 1 << 12;				
			elapsed -= 24*60*60;
		}

		// minutes (12 bits)
		while (elapsed >= 60) {
			clockRegister += 1;
			elapsed -= 60;
		}
		
		if ((clockRegister & 0x0000FFF) >= 24*60)
			clockRegister += (1 << 12) - 24*60;
		
		if ((clockRegister & 0x0FFF000) >= (365 << 12))
			clockRegister += (1 << 24) - (365 << 12);

		clockTime = now - elapsed;
	}
		
	private void setROM(byte[] buffer)
	{
		int banks = buffer.length / ROM_BANK_SIZE;

		if (banks < 2 || banks > 128)
			throw new RuntimeException("Invalid HuC3 ROM size");

		rom = buffer;
		romSize = ROM_BANK_SIZE * banks - 1;
	}

	private void setRAM(byte[] buffer)
	{
		int banks = buffer.length / RAM_BANK_SIZE;

		if (banks < 0 || banks > 4)
			throw new RuntimeException("Invalid HuC3 RAM size");

		ram = buffer;
		ramSize = RAM_BANK_SIZE * banks - 1;
	}
}

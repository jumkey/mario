/**
 * Mario GameBoy (TM) Emulator
 * 
 * Cartridge Controller
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

import gameboy.core.cartridge.CartridgeFactory;
import gameboy.core.cartridge.MBC;
import gameboy.core.driver.ClockDriver;
import gameboy.core.driver.StoreDriver;

public class Cartridge {
	/*
	 * ROM Image
	 */
	private byte[] rom;
	
	/*
	 * Shadow RAM
	 */
	private byte[] ram;
	
	/*
	 * Memory Bank Controller
	 */
	private MBC mbc;

	/*
	 * Cartridge Store
	 */
	private StoreDriver store;
	
	/*
	 * Real Time Clock
	 */
	private ClockDriver clock;
	
	public Cartridge(StoreDriver store, ClockDriver clock)
	{
		this.store = store;
		this.clock = clock;
	}
	
	public final String getTitle()
	{
		byte[] title = new byte[14];

		for (int index = 0; index < title.length; index++)
			title[index] = rom[0x0134 + index];
		
		int length = 0;
		while (length < title.length && title[length] != 0)
			length++;
		
		return new String(title, 0, length);
	}
	
	public final int getCartridgeType()
	{
		return rom[0x0147] & 0xFF;
	}

	public final byte[] getROM()
	{
		return rom;
	}
	
	public final int getROMSize()
	{
		int romSize = rom[0x0148] & 0xFF;
		
		if (romSize >= 0x00 && romSize <= 0x07)
			return 32768 << romSize;
		
		return -1;
	}
	
	public final int getRAMSize()
	{
		int ramSize = 0;

		switch (rom[0x0149]) {
		case 0x00:
			ramSize = 0;
			break;
		case 0x01:
			ramSize = 8192; // FIXME: 2048
			break;
		case 0x02:
			ramSize = 8192;
			break;
		case 0x03:
			ramSize = 32768;
			break;
		}

		return ramSize;
	}
	
	public final int getDestinationCode()
	{
		return rom[0x014A] & 0xFF;
	}

	public final int getLicenseeCode()
	{
		return rom[0x014B] & 0xFF;
	}

	public final int getROMVersion()
	{
		return rom[0x014C] & 0xFF;
	}
	
	public final int getHeaderChecksum()
	{
		return rom[0x014D] & 0xFF;
	}
	
	public final int getChecksum()
	{
		return ((rom[0x014E] & 0xFF) << 8) + (rom[0x014F] & 0xFF);
	}

	public final String getDescription()
	{
		return CartridgeFactory.getCartridgeDescription(getCartridgeType());
	}	
	
	public final boolean hasBattery()
	{
		return CartridgeFactory.hasCartridgeBattery(getCartridgeType());
	}
	
	public final void reset()
	{
		if (!hasBattery()) {
			for (int index = 0; index < ram.length; index++)
				ram[index] = (byte) 0xFF;
		}
		
		mbc.reset();
	}
	
	public final int read(int address)
	{
		return mbc.read(address);
	}

	public final void write(int address, int data)
	{
		mbc.write(address, data);
	}
	
	public final void load(String cartridgeName)
	{
		int romSize = store.getCartridgeSize(cartridgeName);
		
		rom = new byte[romSize];
		
		store.readCartridge(cartridgeName, rom);
		
		if (!verifyHeader())
			throw new RuntimeException("Cartridge header is corrupted");
		
		if (romSize < getROMSize())
			throw new RuntimeException("Cartridge is truncated");
	
		int ramSize = getRAMSize();
		
		if (getCartridgeType() >= CartridgeFactory.TYPE_MBC2 && getCartridgeType() <= CartridgeFactory.TYPE_MBC2_BATTERY)
			ramSize = 512;
		
		ram = new byte[ramSize];
		
		for (int index = 0; index < ramSize; index++)
			ram[index] = (byte) 0xFF;
		
		if (store.hasBattery(cartridgeName))
			store.readBattery(cartridgeName, ram);
		
		mbc = CartridgeFactory.createBankController(getCartridgeType(), rom, ram, clock);
	}
	
	public final void save(String cartridgeName)
	{
		if (hasBattery())
			store.writeBattery(cartridgeName, ram);
	}

	public final boolean verify()
	{
		int checksum = 0;
		
		for (int address = 0; address < rom.length; address++) {
			if (address != 0x014E && address != 0x014F)
				checksum = (checksum + (rom[address] & 0xFF)) & 0xFFFF;
		}
		
		return (checksum == getChecksum());
	}
		
	private final boolean verifyHeader()
	{
		if (rom.length < 0x0150)
			return false;
		
		int checksum = 0xE7;
		
		for (int address = 0x0134; address <= 0x014C; address++) {
			checksum = (checksum - (rom[address] & 0xFF)) & 0xFF;
		}
		
		return (checksum == getHeaderChecksum());
	}
	
	public final String toString()
	{
		return "Title=" + getTitle() + " Type=" + getCartridgeType() + " Destination=" + getDestinationCode();
	}
}

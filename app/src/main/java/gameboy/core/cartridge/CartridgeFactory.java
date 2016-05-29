/**
 * Mario GameBoy (TM) Emulator
 * 
 * Cartridge Factory
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

public class CartridgeFactory {
	/*
	 * Cartridge Types
	 */
	public static final int TYPE_ROM_ONLY					= 0x00;
	
	public static final int TYPE_MBC1						= 0x01;
	public static final int TYPE_MBC1_RAM					= 0x02;
	public static final int TYPE_MBC1_RAM_BATTERY			= 0x03;
	
	public static final int TYPE_MBC2						= 0x05;
	public static final int TYPE_MBC2_BATTERY				= 0x06;
	
	public static final int TYPE_MBC3_RTC_BATTERY			= 0x0F;
	public static final int TYPE_MBC3_RTC_RAM_BATTERY		= 0x10;
	public static final int TYPE_MBC3						= 0x11;
	public static final int TYPE_MBC3_RAM					= 0x12;
	public static final int TYPE_MBC3_RAM_BATTERY			= 0x13;
	
	public static final int TYPE_MBC5						= 0x19;
	public static final int TYPE_MBC5_RAM					= 0x1A;
	public static final int TYPE_MBC5_RAM_BATTERY			= 0x1B;
	
	public static final int TYPE_MBC5_RUMBLE				= 0x1C;
	public static final int TYPE_MBC5_RUMBLE_RAM			= 0x1D;
	public static final int TYPE_MBC5_RUMBLE_RAM_BATTERY	= 0x1E;

	public static final int TYPE_HUC3_RTC_RAM				= 0xFE;
	public static final int TYPE_HUC1_RAM_BATTERY			= 0xFF;
	
	
	public static final boolean hasCartridgeBattery(int cartridgeType)
	{
		return (cartridgeType == TYPE_MBC1_RAM_BATTERY ||
				cartridgeType == TYPE_MBC2_BATTERY ||
				cartridgeType == TYPE_MBC3_RTC_BATTERY ||
				cartridgeType == TYPE_MBC3_RTC_RAM_BATTERY ||
				cartridgeType == TYPE_MBC3_RAM_BATTERY ||
				cartridgeType == TYPE_MBC5_RAM_BATTERY ||
				cartridgeType == TYPE_MBC5_RUMBLE_RAM_BATTERY ||
				cartridgeType == TYPE_HUC1_RAM_BATTERY);
	}
	
	public static final String getCartridgeDescription(int cartridgeType)
	{
		switch (cartridgeType) {
		case TYPE_ROM_ONLY:
			return "ROM ONLY";

		case TYPE_MBC1:
			return "MBC1";
			
		case TYPE_MBC1_RAM:
			return "MBC1+RAM";
			
		case TYPE_MBC1_RAM_BATTERY:
			return "MBC1+RAM+BATTERY";

		case TYPE_MBC2:
			return "MBC2";
			
		case TYPE_MBC2_BATTERY:
			return "MBC2+BATTERY";
		
		case TYPE_MBC3_RTC_BATTERY:
			return "MBC3+RTC+BATTERY";
			
		case TYPE_MBC3_RTC_RAM_BATTERY:
			return "MBC3+RTC+RAM+BATTERY";
			
		case TYPE_MBC3:
			return "MBC3";
			
		case TYPE_MBC3_RAM:
			return "MBC3+RAM";
			
		case TYPE_MBC3_RAM_BATTERY:
			return "MBC3+RAM+BATTERY";
			
		case TYPE_MBC5:
			return "MBC5";
			
		case TYPE_MBC5_RAM:
			return "MBC5+RAM";
			
		case TYPE_MBC5_RAM_BATTERY:
			return "MBC5+RAM+BATTERY";
			
		case TYPE_MBC5_RUMBLE:
			return "MBC5+RUMBLE";
			
		case TYPE_MBC5_RUMBLE_RAM:
			return "MBC5+RUMBLE+RAM";
			
		case TYPE_MBC5_RUMBLE_RAM_BATTERY:
			return "MBC5+RUMBLE+RAM+BATTERY";

		case TYPE_HUC3_RTC_RAM:
			return "HuC3+RTC+RAM";
			
		case TYPE_HUC1_RAM_BATTERY:
			return "HuC1+RAM+BATTERY";
			
		default:
			return "UNKNOWN";
		}
	}
	
	public static final MBC createBankController(int cartridgeType, byte[] rom, byte[] ram, ClockDriver clock)
	{
		switch (cartridgeType) {
		case TYPE_ROM_ONLY:
		case TYPE_MBC1:
		case TYPE_MBC1_RAM:
		case TYPE_MBC1_RAM_BATTERY:
			return new MBC1(rom, ram);

		case TYPE_MBC2:
		case TYPE_MBC2_BATTERY:
			return new MBC2(rom, ram);
			
		case TYPE_MBC3_RTC_BATTERY:
		case TYPE_MBC3_RTC_RAM_BATTERY:
		case TYPE_MBC3:
		case TYPE_MBC3_RAM:
		case TYPE_MBC3_RAM_BATTERY:
			return new MBC3(rom, ram, clock);
			
		case TYPE_MBC5:
		case TYPE_MBC5_RAM:
		case TYPE_MBC5_RAM_BATTERY:
			return new MBC5(rom, ram, false);
			
		case TYPE_MBC5_RUMBLE:
		case TYPE_MBC5_RUMBLE_RAM:
		case TYPE_MBC5_RUMBLE_RAM_BATTERY:
			return new MBC5(rom, ram, true);

		case TYPE_HUC3_RTC_RAM:
			return new HuC3(rom, ram, clock);
			
		case TYPE_HUC1_RAM_BATTERY:
			return new HuC1(rom, ram);
			
		case 0xEA: // HACK: Sonic 3D Blast 5
			return new MBC1(rom, ram);
			
		default:
			throw new RuntimeException("Unsupported memory bank controller (0x" + Integer.toHexString(cartridgeType) + ")");
		}
	}
	
}

/**
 * Mario GameBoy (TM) Emulator
 * 
 * Cartridge Store Driver
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
package gameboy.core.driver;

public interface StoreDriver {
	public boolean hasCartridge(String cartridgeName);
	
	public int getCartridgeSize(String cartridgeName);

	public void readCartridge(String cartridgeName, byte[] buffer);

	public boolean hasBattery(String cartridgeName);
	
	public int getBatterySize(String cartridgeName);
	
	public void readBattery(String cartridgeName, byte[] buffer);
	
	public void writeBattery(String cartridgeName, byte[] buffer);

	public void removeBattery(String cartridgeName);
}
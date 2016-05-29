/**
 * Mario GameBoy (TM) Emulator
 * 
 * J2ME MIDP 2.0 Cartridge Store
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
package gameboy.platform.android;

import java.io.IOException;
import java.io.InputStream;

import gameboy.core.driver.StoreDriver;

public class Store implements StoreDriver {
	public Store()
	{
	}
	
	public boolean hasCartridge(String cartridgeName)
	{
		InputStream input = getResource(cartridgeName);
		
		if (input != null) {
			try {
				input.close();
			}
			catch (IOException exception) {
			}
			return true;
		}
		
		return false;
	}

	public int getCartridgeSize(String cartridgeName)
	{
		try {
			InputStream input = getResource(cartridgeName);
			
			try {
				byte[] buffer = new byte[1024];
				int length, size = 0;
				
				while ((length = input.read(buffer, 0, buffer.length)) > 0)
					size += length;
				
				return size;
			}
			finally {
				input.close();
			}
		}
		catch (IOException exception) {
			throw new RuntimeException("Could not get cartridge size: " + cartridgeName);
		}
	}

	public void readCartridge(String cartridgeName, byte[] buffer)
	{
		try {
			InputStream input = getResource(cartridgeName);
			
			try {

				//FIXME read(buffer, 0, buffer.length) 不一定会读取满
				int length, i=0;
				while ((length = input.read(buffer, i, buffer.length-i)) > 0)
					i += length;
				if (i != buffer.length)
					throw new IOException("Unexpected end of resource");
				//if (input.read(buffer, 0, buffer.length) != buffer.length)
				//	throw new IOException("Unexpected end of resource");

			}
			finally {
				input.close();
			}
		}
		catch (IOException exception) {
			throw new RuntimeException("Could not load cartridge: " + cartridgeName);
		}
	}

	public boolean hasBattery(String cartridgeName)
	{
		// TODO
		return false;
	}

	public int getBatterySize(String cartridgeName)
	{
		// TODO
		return 0;
	}

	public void readBattery(String cartridgeName, byte[] buffer)
	{
		// TODO
	}

	public void writeBattery(String cartridgeName, byte[] buffer)
	{
		// TODO
	}

	public void removeBattery(String cartridgeName)
	{
		// TODO
	}


	private InputStream getResource(String resourceName)
	{
		return getClass().getResourceAsStream(resourceName);
	}
}

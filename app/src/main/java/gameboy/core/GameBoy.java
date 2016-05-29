/**
 * Mario GameBoy (TM) Emulator
 * 
 * Gameboy Scheduler and Memory Mapper
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

import gameboy.core.driver.ClockDriver;
import gameboy.core.driver.StoreDriver;
import gameboy.core.driver.JoypadDriver;
import gameboy.core.driver.SoundDriver;
import gameboy.core.driver.VideoDriver;

public class GameBoy implements Memory {
	/*
	 * Registered Symbol
	 */
	private static final byte REGISTERED_BITMAP[] = {
		(byte) 0x3C, (byte) 0x42, (byte) 0xB9, (byte) 0xA5, (byte) 0xB9, (byte) 0xA5, (byte) 0x42, (byte) 0x3C
	};
		
	private RAM ram;
	private Cartridge cartridge;
	private Interrupt interrupt;
	private CPU cpu;
	private Serial serial;
	private Timer timer;
	private Joypad joypad;
	private Video video;
	private Sound sound;
	
	public GameBoy(VideoDriver videoDriver, SoundDriver soundDriver, JoypadDriver joypadDriver, StoreDriver storeDriver, ClockDriver clockDriver)
	{
		ram = new RAM();
		cartridge = new Cartridge(storeDriver, clockDriver);
		interrupt = new Interrupt();
		cpu = new CPU(interrupt, this);
		serial = new Serial(interrupt);
		timer = new Timer(interrupt);
		joypad = new Joypad(joypadDriver, interrupt);
		video = new Video(videoDriver, interrupt, this);
		sound = new Sound(soundDriver);
	}

	public final Cartridge getCartridge()
	{
		return cartridge;
	}
	
	public final int getFrameSkip()
	{
		return video.getFrameSkip();		
	}

	public final void setFrameSkip(int frameSkip)
	{
		video.setFrameSkip(frameSkip);
	}
	
	public final void load(String cartridgeName)
	{
		cartridge.load(cartridgeName);
	}
	
	public final void save(String cartridgeName)
	{
		cartridge.save(cartridgeName);
	}
	
	public final void start()
	{
		sound.start();
	}
	
	public final void stop()
	{
		sound.stop();
	}

	public final void reset()
	{
		ram.reset();
		cartridge.reset();
		interrupt.reset();
		cpu.reset();
		serial.reset();
		timer.reset();
		joypad.reset();
		video.reset();
		sound.reset();
		
		cpu.setROM(cartridge.getROM());
		
		drawLogo();
	}

	public final int cycles()
	{
		return Math.min(Math.min(Math.min(Math.min(video.cycles(), serial.cycles()), timer.cycles()), sound.cycles()), joypad.cycles());
	}
	
	public final void emulate(int ticks)
	{
		while (ticks > 0) {
			int count = cycles();

			cpu.emulate(count);
			serial.emulate(count);
			timer.emulate(count);
			video.emulate(count);
			sound.emulate(count);
			joypad.emulate(count);
			
			ticks -= count;
		}
	}

	public final void write(int address, int data)
	{
		if (address <= 0x7FFF) {
			// 0000-7FFF ROM Bank
			cartridge.write(address, data);
		}
		else if (address <= 0x9FFF) {
			// 8000-9FFF Video RAM
			video.write(address, data);
		}
		else if (address <= 0xBFFF) {
			// A000-BFFF External RAM
			cartridge.write(address, data);
		}
		else if (address <= 0xFDFF) {
			// C000-FDFF Work RAM
			ram.write(address, data);
		}
		else if (address <= 0xFEFF) {
			// FE00-FEFF OAM
			video.write(address, data);
		}
		else if (address == 0xFF00) {
			// FF00-FF00 Joypad
			joypad.write(address, data);
		}
		else if (address >= 0xFF01 && address <= 0xFF02) {
			// FF01-FF02 Serial
			serial.write(address, data);
		}
		else if (address >= 0xFF04 && address <= 0xFF07) {
			// FF04-FF07 Timer
			timer.write(address, data);
		}
		else if (address == 0xFF0F) {
			// FF0F-FF0F Interrupt
			interrupt.write(address, data);
			
			// check pending interrupts when IF is changed
			cpu.interrupt();
		}
		else if (address >= 0xFF10 && address <= 0xFF3F) {
			// FF10-FF3F Sound
			sound.write(address, data); 
		}
		else if (address >= 0xFF40 && address <= 0xFF4B) {
			// FF40-FF4B Video
			video.write(address, data);
			
			// check pending interrupts when STAT is changed
			if (address == Video.STAT)
				cpu.interrupt();
		}
		else if (address >= 0xFF80 && address <= 0xFFFE) {
			// FF80-FFFE High RAM
			ram.write(address, data);
		}
		else if (address == 0xFFFF) {
			// FFFF-FFFF Interrupt
			interrupt.write(address, data);
			
			// check pending interrupts when IE is changed
			cpu.interrupt();
		}
	}
	
	public final int read(int address)
	{
		if (address <= 0x7FFF) {
			// 0000-7FFF ROM Bank
			return cartridge.read(address);
		}
		else if (address <= 0x9FFF) {
			// 8000-9FFF Video RAM
			return video.read(address);
		}
		else if (address <= 0xBFFF) {
			// A000-BFFF External RAM
			return cartridge.read(address);
		}
		else if (address <= 0xFDFF) {
			// C000-FDFF Work RAM
			return ram.read(address);
		}
		else if (address <= 0xFEFF) {
			// FE00-FEFF OAM
			return video.read(address);
		}
		else if (address == 0xFF00) {
			// FF00-FF00 Joypad
			return joypad.read(address);
		}
		else if (address >= 0xFF01 && address <= 0xFF02) {
			// FF01-FF02 Serial
			return serial.read(address);
		}
		else if (address >= 0xFF04 && address <= 0xFF07) {
			// FF04-FF07 Timer
			return timer.read(address);
		}
		else if (address == 0xFF0F) {
			// FF0F-FF0F Interrupt
			return interrupt.read(address);
		}
		else if (address >= 0xFF10 && address <= 0xFF3F) {
			// FF10-FF3F Sound
			return sound.read(address); 
		}
		else if (address >= 0xFF40 && address <= 0xFF4B) {
			// FF40-FF4B Video
			return video.read(address);
		}
		else if (address >= 0xFF80 && address <= 0xFFFE) {
			// FF80-FFFE High RAM
			return ram.read(address);
		}
		else if (address == 0xFFFF) {
			// FFFF-FFFF Interrupt
			return interrupt.read(address);
		}
		else {
			return 0xFF;
		}
	}
	
	private final void drawLogo()
	{
		for (int index = 0; index < 48; index++) {
			int bits = cartridge.read(0x0104 + index);

			int pattern0 = ((bits >> 0) & 0x80) +
					       ((bits >> 1) & 0x60) +
					       ((bits >> 2) & 0x18) +
					       ((bits >> 3) & 0x06) +
					       ((bits >> 4) & 0x01);
		
			int pattern1 = ((bits << 4) & 0x80) +
						   ((bits << 3) & 0x60) +
						   ((bits << 2) & 0x18) +
						   ((bits << 1) & 0x06) +
						   ((bits << 0) & 0x01);

			video.write(0x8010 + (index << 3), pattern0);
			video.write(0x8012 + (index << 3), pattern0);
			
			video.write(0x8014 + (index << 3), pattern1);
			video.write(0x8016 + (index << 3), pattern1);
		}
		
		for (int index = 0; index < 8; index++) {
			video.write(0x8190 + (index << 1), REGISTERED_BITMAP[index]);
		}
		
		for (int tile = 0; tile < 12; tile++) {
			video.write(0x9904 + tile, tile + 1);
			video.write(0x9924 + tile, tile + 13);
		}
		
		video.write(0x9904 + 12, 25);
	}
}

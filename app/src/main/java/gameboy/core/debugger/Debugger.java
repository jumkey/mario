/**
 * Mario GameBoy (TM) Emulator
 * 
 * Debugger
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
package gameboy.core.debugger;

import gameboy.core.CPU;
import gameboy.core.Interrupt;
import gameboy.core.Joypad;
import gameboy.core.Memory;
import gameboy.core.Serial;
import gameboy.core.Sound;
import gameboy.core.Timer;
import gameboy.core.Video;

public class Debugger {
	private static final String hexdigits[] = {
		"0", "1", "2", "3", "4", "5", "6", "7",
		"8", "9", "A", "B", "C", "D", "E", "F"
	};
	
	private CPU cpu;
	private Memory memory;
	private Disassembler disassembler;
	private boolean enabled;
		
	public Debugger(CPU cpu, Memory memory)
	{
		this.cpu = cpu;
		this.memory = memory;
		this.disassembler = new Disassembler(memory);
		this.enabled = false;
	}
	
	public final boolean isEnabled()
	{
		return enabled;
	}
	
	public final void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
	
	public final void execute()
	{
		println("A=" + hexByte(cpu.getAF() >> 8) +
			    " BC=" + hexWord(cpu.getBC()) +
		        " DE=" + hexWord(cpu.getDE()) +
		        " HL=" + hexWord(cpu.getHL()) +
		        " SP=" + hexWord(cpu.getSP()) +
			    " F=" + ((cpu.getAF() & CPU.Z_FLAG) != 0 ? "Z" : "-") +
	            	    ((cpu.getAF() & CPU.N_FLAG) != 0 ? "N" : "-") +
	           		    ((cpu.getAF() & CPU.H_FLAG) != 0 ? "H" : "-") +
	           		    ((cpu.getAF() & CPU.C_FLAG) != 0 ? "C" : "-") +
		        " I=" + cpu.getIF() +
		        " " + disassembler.disassemble(cpu.getPC()));
	}
	
	public final void interrupt(int address)
	{
		println("INTERRUPT " + hexWord(address));
		
		printInterrupt();
	}
	
	public final void write(int address, int data)
	{
		if (address >= 0xFF00 && address <= 0xFF7F)
			println("WRITE " + hexWord(address) + " " + hexByte(data));
		
		printRegisters(address);
	}

	public final void read(int address)
	{
		if (address >= 0xFF00 && address <= 0xFF7F)
			println("READ " + hexWord(address));

		printRegisters(address);
	}
	
	private final void printRegisters(int address)
	{
		if (address == 0xFF00) {
			printJoypad();
		}
		else if (address >= 0xFF01 && address <= 0xFF02) {
			printSerial();
		}
		else if (address >= 0xFF04 && address <= 0xFF07) {
			printTimer();
		}
		else if (address == 0xFF0F || address == 0xFFFF) {
			printInterrupt();
		}
		else if (address >= 0xFF10 && address <= 0xFF2F) {
			printSound();
		}
		else if (address >= 0xFF40 && address <= 0xFF4F) {
			printVideo();
		}
	}
		
	private final void printTimer()
	{
		println("DIV=" + hexByte(memory.read(Timer.DIV)) +
		        " TIMA=" + hexByte(memory.read(Timer.TIMA)) +
		        " TMA=" + hexByte(memory.read(Timer.TMA)) +
		        " TAC=" + hexByte(memory.read(Timer.TAC)));
	}
	
	private final void printSerial()
	{
		println("SB=" + hexByte(memory.read(Serial.SB)) +
				" SC=" + hexByte(memory.read(Serial.SC)));
	}
	
	private final void printJoypad()
	{
		println("JOYP=" + hexByte(memory.read(Joypad.JOYP)));
	}
	
	private final void printSound()
	{
		println("NR10=" + hexByte(memory.read(Sound.NR10)) +
				" NR11=" + hexByte(memory.read(Sound.NR11)) +
				" NR12=" + hexByte(memory.read(Sound.NR12)) +
				" NR13=" + hexByte(memory.read(Sound.NR13)) +
				" NR14=" + hexByte(memory.read(Sound.NR14)) +
				
				"  NR21=" + hexByte(memory.read(Sound.NR21)) +
				" NR22=" + hexByte(memory.read(Sound.NR22)) +
				" NR23=" + hexByte(memory.read(Sound.NR23)) +
				" NR24=" + hexByte(memory.read(Sound.NR24)) +
				 
				"  NR50=" + hexByte(memory.read(Sound.NR50)) + 
				" NR51=" + hexByte(memory.read(Sound.NR51)));

		println("NR30=" + hexByte(memory.read(Sound.NR30)) +
				" NR31=" + hexByte(memory.read(Sound.NR31)) +
				" NR32=" + hexByte(memory.read(Sound.NR32)) +
				" NR33=" + hexByte(memory.read(Sound.NR33)) +
				" NR34=" + hexByte(memory.read(Sound.NR34)) +

			    "  NR41=" + hexByte(memory.read(Sound.NR41)) +
				" NR42=" + hexByte(memory.read(Sound.NR42)) +
				" NR43=" + hexByte(memory.read(Sound.NR43))	+
				" NR44=" + hexByte(memory.read(Sound.NR44)) +
				
			    "  NR52=" + hexByte(memory.read(Sound.NR52)));
	}
	
	private final void printVideo()
	{
		println("LCDC=" + hexByte(memory.read(Video.LCDC)) +
				" STAT=" + hexByte(memory.read(Video.STAT)) +
				" SCY=" + hexByte(memory.read(Video.SCY)) +
				" SCX=" + hexByte(memory.read(Video.SCX)) +
				" LY=" + hexByte(memory.read(Video.LY)) +
				" LYC=" + hexByte(memory.read(Video.LYC)) +
				" DMA=" + hexByte(memory.read(Video.DMA)) +
				" BGP=" + hexByte(memory.read(Video.BGP)) +
				" OBP=" + hexByte(memory.read(Video.OBP0)) + "/" + hexByte(memory.read(Video.OBP1)) +
				" WY=" + hexByte(memory.read(Video.WY)) +
				" WX=" + hexByte(memory.read(Video.WX)));
	}

	private final void printInterrupt()
	{
		println("IE=" + hexByte(memory.read(Interrupt.IE)) +
				" IF=" + hexByte((byte)memory.read(Interrupt.IF)));
	}
	
	private static final void println(String message)
	{
		System.out.println(message);
	}
	
	private static final String hexWord(int value)
	{
		return hexdigits[(value >> 12) & 0x0F] + hexdigits[(value >> 8) & 0x0F] + hexdigits[(value >> 4) & 0x0F] + hexdigits[value & 0x0F]; 
	}
	
	private static final String hexByte(int value)
	{
		return hexdigits[(value >> 4) & 0x0F] + hexdigits[value & 0x0F]; 
	}
}

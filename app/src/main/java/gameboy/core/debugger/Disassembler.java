/**
 * Mario GameBoy (TM) Emulator
 * 
 * Disassembler
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

import gameboy.core.Memory;

public class Disassembler {
	private static final String registers[]	= {
		"B", "C", "D", "E", "H", "L", "(HL)", "A",
		"BC", "DE", "HL", "SP", "BC", "DE", "HL", "AF"
	};

	private static final String conditions[] = {
		"NZ", "Z", "NC", "C"
	};

	private static final String hexdigits[] = {
		"0", "1", "2", "3", "4", "5", "6", "7",
		"8", "9", "A", "B", "C", "D", "E", "F"
	};
	
	private Memory memory;
	private int address;
	
	public Disassembler(Memory memory)
	{
		this.memory = memory;
		this.address = 0x100;
	}

	public int getAddress()
	{
		return address;
	}
	
	public void setAddress(int address)
	{
		this.address = address;
	}
	
	public String disassemble(int address)
	{
		setAddress(address);
		
		return disassemble();
	}
	
	public String disassemble()
	{
		int opcode = read(address);
		
		if (opcode == 0x00) {
			// 00|000|000
			return format("NOP");
		}
		else if (opcode == 0x08) {
			// 00|001|000
			return format("LD (a),SP");
		}
		else if (opcode == 0x10) {
			// 00|010|000
			return format("STOP n");
		}
		else if (opcode == 0x18) {
			// 00|011|000
			return format("JR d");
		}
		else if ((opcode & 0xE7) == 0x20) {
			// 00|1cc|000
			return format("JR c,d");
		}
		else if ((opcode & 0xCF) == 0x01) {
			// 00|pp0|001
			return format("LD p,m");
		}
		else if ((opcode & 0xCF) == 0x09) {
			// 00|pp1|001
			return format("ADD HL,p");
		}
		else if (opcode == 0x02) {
			// 00|000|010
			return format("LD (BC),A");
		}
		else if (opcode == 0x0A) {
			// 00|001|010
			return format("LD A,(BC)");
		}
		else if (opcode == 0x12) {
			// 00|010|010
			return format("LD (DE),A");
		}
		else if (opcode == 0x1A) {
			// 00|011|010
			return format("LD A,(DE)");
		}
		else if (opcode == 0x22) {
			// 00|100|010
			return format("LDI (HL),A");
		}
		else if (opcode == 0x2A) {
			// 00|101|010
			return format("LDI A,(HL)");
		}
		else if (opcode == 0x32) {
			// 00|110|010
			return format("LDD (HL),A");
		}
		else if (opcode == 0x3A) {
			// 00|111|010
			return format("LDD A,(HL)");
		}
		else if ((opcode & 0xCF) == 0x03) {
			// 00|pp0|011
			return format("INC p");
		}
		else if ((opcode & 0xCF) == 0x0B) {
			// 00|pp1|011
			return format("DEC p");
		}
		else if ((opcode & 0xC7) == 0x04) {
			// 00|rrr|100
			return format("INC r");
		}
		else if ((opcode & 0xC7) == 0x05) {
			// 00|rrr|101
			return format("DEC r");
		}
		else if ((opcode & 0xC7) == 0x06) {
			// 00|rrr|110
			return format("LD r,n");
		}
		else if (opcode == 0x07) {
			// 00|000|111
			return format("RLCA");
		}
		else if (opcode == 0x0F) {
			// 00|001|111
			return format("RRCA");
		}
		else if (opcode == 0x17) {
			// 00|010|111
			return format("RLA");
		}
		else if (opcode == 0x1F) {
			// 00|011|111
			return format("RRA");
		}
		else if (opcode == 0x27) {
			// 00|100|111
			return format("DAA");
		}
		else if (opcode == 0x2F) {
			// 00|101|111
			return format("CPL");
		}
		else if (opcode == 0x37) {
			// 00|110|111
			return format("SCF");
		}
		else if (opcode == 0x3F) {
			// 00|111|111
			return format("CCF");
		}
		else if (opcode == 0x76) {
			// 01|110|110
			return format("HALT");
		}
		else if ((opcode & 0xC0) == 0x40) {
			// 01|rrr|sss
			return format("LD r,s");
		}
		else if ((opcode & 0xF8) == 0x80) {
			// 10|000|rrr
			return format("ADD A,s");
		}
		else if ((opcode & 0xF8) == 0x88) {
			// 10|001|rrr
			return format("ADC A,s");
		}
		else if ((opcode & 0xF8) == 0x90) {
			// 10|010|rrr
			return format("SUB A,s");
		}
		else if ((opcode & 0xF8) == 0x98) {
			// 10|011|rrr
			return format("SBC A,s");
		}
		else if ((opcode & 0xF8) == 0xA0) {
			// 10|100|rrr
			return format("AND A,s");
		}
		else if ((opcode & 0xF8) == 0xA8) {
			// 10|101|rrr
			return format("XOR A,s");
		}
		else if ((opcode & 0xF8) == 0xB0) {
			// 10|110|rrr
			return format("OR A,s");
		}
		else if ((opcode & 0xF8) == 0xB8) {
			// 10|111|rrr
			return format("CP A,s");
		}
		else if ((opcode & 0xE7) == 0xC0) {
			// 11|0cc|000
			return format("RET c");
		}
		else if (opcode == 0xE0) {
			// 11|100|000
			return format("LD (h),A");
		}
		else if (opcode == 0xE8) {
			// 11|101|000
			return format("ADD SP,n");
		}
		else if (opcode == 0xF0) {
			// 11|110|000
			return format("LD A,(h)");
		}
		else if (opcode == 0xF8) {
			// 11|111|000
			return format("LD HL,SP+n");
		}
		else if ((opcode & 0xCF) == 0xC1) {
			// 11|qq0|001
			return format("POP q");
		}
		else if (opcode == 0xC9) {
			// 11|001|001
			return format("RET");
		}
		else if (opcode == 0xD9) {
			// 11|011|001
			return format("RETI");
		}
		else if (opcode == 0xE9) {
			// 11|101|001
			return format("LD PC,HL");
		}
		else if (opcode == 0xF9) {
			// 11|111|001
			return format("LD SP,HL");
		}
		else if ((opcode & 0xE7) == 0xC2) {
			// 11|0cc|010
			return format("JP c,m");
		}
		else if (opcode == 0xE2) {
			// 11|100|010
			return format("LD (FF00+C),A");
		}
		else if (opcode == 0xEA) {
			// 11|101|010
			return format("LD (a),A");
		}
		else if (opcode == 0xF2) {
			// 11|110|010
			return format("LD A,(FF00+C)");
		}
		else if (opcode == 0xFA) {
			// 11|111|010
			return format("LD A,(a)");
		}
		else if (opcode == 0xC3) {
			// 11|000|011
			return format("JP m");
		}
		else if (opcode == 0xCB) {
			// 11|001|011
			opcode = read(address + 1);
			
			if ((opcode & 0xF8) == 0x00) {
				// 00|000|rrr
				return format("RLC s");
			}
			else if ((opcode & 0xF8) == 0x08) {
				// 00|001|rrr
				return format("RRC s");
			}
			else if ((opcode & 0xF8) == 0x10) {
				// 00|010|rrr
				return format("RL s");
			}
			else if ((opcode & 0xF8) == 0x18) {
				// 00|011|rrr
				return format("RR s");
			}
			else if ((opcode & 0xF8) == 0x20) {
				// 00|100|rrr
				return format("SLA s");
			}
			else if ((opcode & 0xF8) == 0x28) {
				// 00|101|rrr
				return format("SRA s");
			}
			else if ((opcode & 0xF8) == 0x30) {
				// 00|110|rrr
				return format("SWAP s");
			}
			else if ((opcode & 0xF8) == 0x38) {
				// 00|111|rrr
				return format("SRL s");
			}
			else if ((opcode & 0xC0) == 0x40) {
				// 01|iii|rrr
				return format("BIT i,s");
			}
			else if ((opcode & 0xC0) == 0x80) {
				// 10|iii|rrr
				return format("RES i,s");
			}
			else {
				// 11|iii|rrr
				return format("SET i,s");
			}
		}
		else if (opcode == 0xF3) {
			// 11|110|011
			return format("DI");
		}
		else if (opcode == 0xFB) {
			// 11|111|011
			return format("EI");
		}
		else if ((opcode & 0xE7) == 0xC4) {
			// 11|0cc|100
			return format("CALL c,m");
		}
		else if ((opcode & 0xCF) == 0xC5) {
			// 11|qq0|101
			return format("PUSH q");
		}
		else if (opcode == 0xCD) {
			// 11|001|101
			return format("CALL m");
		}
		else if (opcode == 0xC6) {
			// 11|000|110
			return format("ADD A,n");
		}
		else if (opcode == 0xCE) {
			// 11|001|110
			return format("ADC A,n");
		}
		else if (opcode == 0xD6) {
			// 11|010|110
			return format("SUB A,n");
		}
		else if (opcode == 0xDE) {
			// 11|011|110
			return format("SBC A,n");
		}
		else if (opcode == 0xE6) {
			// 11|100|110
			return format("AND A,n");
		}
		else if (opcode == 0xEE) {
			// 11|101|110
			return format("XOR A,n");
		}
		else if (opcode == 0xF6) {
			// 11|110|110
			return format("OR A,n");
		}
		else if (opcode == 0xFE) {
			// 11|111|110
			return format("CP A,n");
		}
		else if ((opcode & 0xC7) == 0xC7) {
			// 11|jjj|111
			return format("RST j");
		}
		else {
			// 11|010|011
			// 11|011|011
			// 11|100|011
			// 11|101|011
			// 
			// 11|100|100
			// 11|101|100
			// 11|110|100
			// 11|111|100
			//
			// 11|011|101
			// 11|101|101
			// 11|111|101
			return format("??");
		}
	}

	private int read(int address)
	{
		return memory.read(address & 0xFFFF);
	}

	private String format(String code)
	{
		int pc = address;
		
		int opcode = read(address++);
		
		if (opcode == 0xCB)
			opcode = read(address++);			

		if (code.contains("r"))
			code = code.replace("r", registers[(opcode >> 3) & 7]);
		
		if (code.contains("s"))
			code = code.replace("s", registers[(opcode >> 0) & 7]);

		if (code.contains("p"))
			code = code.replace("p", registers[((opcode >> 4) & 3) + 8]);
		
		if (code.contains("q"))
			code = code.replace("q", registers[((opcode >> 4) & 3) + 12]);
		
		if (code.contains("c"))
			code = code.replace("c", conditions[(opcode >> 3) & 3]);
		
		if (code.contains("i"))
			code = code.replace("i", hexdigits[(opcode >> 3) & 7]);

		if (code.contains("j"))
			code = code.replace("j", hexadecimal(opcode & 0x38));

		if (code.contains("d"))
			code = code.replace("d", hexadecimal((byte) read(address++) + address));
		
		if (code.contains("n"))
			code = code.replace("n", hexadecimal(read(address++)));
		
		if (code.contains("m"))
			code = code.replace("m", hexadecimal(read(address++) + (read(address++) << 8)));

		if (code.contains("a"))
			code = code.replace("a", memoryAddress(read(address++) + (read(address++) << 8)));

		if (code.contains("h"))
			code = code.replace("h", memoryAddress(read(address++) + 0xFF00));
		
		String prefix = hexadecimal(pc >> 8) + hexadecimal(pc & 0xFF) + " ";
		
		while (pc < address)
			prefix += hexadecimal(read(pc++));
		
		while (prefix.length() < 12)
			prefix += "  ";

		return prefix + code;
	}
	
	
	private static String hexadecimal(int value)
	{
		if (value <= 0xFF)
			return hexdigits[(value >> 4) & 0x0F] + hexdigits[value & 0x0F];
		
		return hexadecimal(value >> 8) + hexadecimal(value & 0xFF);
	}
	
	private static String memoryAddress(int address)
	{
		if (address >= 0xFF00) {
			switch (address) {
			// Joypad
			case 0xFF00:
				return "JOYP";
				
			// Serial
			case 0xFF01:
				return "SB";
			case 0xFF02:
				return "SC";
			
			// Timer
			case 0xFF04:
				return "DIV";
			case 0xFF05:
				return "TIMA";
			case 0xFF06:
				return "TMA";
			case 0xFF07:
				return "TAC";
				
			// Interrupt
			case 0xFF0F:
				return "IF";

			// Sound
			case 0xFF10:
				return "NR10";
			case 0xFF11:
				return "NR11";
			case 0xFF12:
				return "NR12";
			case 0xFF13:
				return "NR13";
			case 0xFF14:
				return "NR14";
				
			case 0xFF16:
				return "NR21";
			case 0xFF17:
				return "NR22";
			case 0xFF18:
				return "NR23";
			case 0xFF19:
				return "NR24";
				
			case 0xFF1A:
				return "NR30";
			case 0xFF1B:
				return "NR31";
			case 0xFF1C:
				return "NR32";
			case 0xFF1D:
				return "NR33";
			case 0xFF1E:
				return "NR34";

			case 0xFF20:
				return "NR41";
			case 0xFF21:
				return "NR42";
			case 0xFF22:
				return "NR43";
			case 0xFF23:
				return "NR44";

			case 0xFF24:
				return "NR50";
			case 0xFF25:
				return "NR51";
			case 0xFF26:
				return "NR52";

			// Video
			case 0xFF40:
				return "LCDC";
			case 0xFF41:
				return "STAT";
			case 0xFF42:
				return "SCY";
			case 0xFF43:
				return "SCX";
			case 0xFF44:
				return "LY";
			case 0xFF45:
				return "LYC";
			case 0xFF46:
				return "DMA";
			case 0xFF47:
				return "BGP";
			case 0xFF48:
				return "OBP0";
			case 0xFF49:
				return "OBP1";
			case 0xFF4A:
				return "WY";
			case 0xFF4B:
				return "WX";

			// Interrupt
			case 0xFFFF:
				return "IE";
			}
		}
		
		return hexadecimal(address >> 8) + hexadecimal(address & 0xFF);
	}
}

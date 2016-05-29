/**
 * Mario GameBoy (TM) Emulator
 * 
 * Central Unit Processor (Sharp LR35902 CPU)
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


public final class CPU {
	/*
	 * Flags
	 */
	public static final int Z_FLAG = 0x80;
	public static final int N_FLAG = 0x40;
	public static final int H_FLAG = 0x20;
	public static final int C_FLAG = 0x10;

	/*
	 * Registers
	 */
	private int a, f;
	private int b, c;
	private int d, e;
	private int h, l;
	private int sp;
	private int pc;
	
	/*
	 * Interrupt Flags
	 */
	private boolean ime;
	private boolean halted;
	
	private int cycles;

	/*
	 * Interrupt Controller
	 */
	private Interrupt interrupt;

	/*
	 * Memory Access
	 */
	private Memory memory;

	/*
	 * ROM Access
	 */
	private byte[] rom;
	
	public CPU(Interrupt interrupt, Memory memory)
	{
		this.interrupt = interrupt;
		this.memory = memory;
		
		reset();
	}

	public final int getBC()
	{
		return (b << 8) + c;
	}
	
	public final int getDE()
	{
		return (d << 8) + e;
	}
	
	public final int getHL()
	{
		return (h << 8) + l;
	}
	
	public final int getSP()
	{
		return sp;
	}
	
	public final int getPC()
	{
		return pc;
	}
	
	public final int getAF()
	{
		return (a << 8) + f;
	}

	public final int getIF()
	{
		return (ime ? 0x01 : 0x00) + (halted ? 0x80 : 0x00);
	}
	
	public final void setROM(byte[] banks)
	{
		rom = banks;
	}
	
	public final void reset()
	{
		a = 0x01;
		f = 0x80;
		b = 0x00;
		c = 0x13;
		d = 0x00;
		e = 0xD8;
		h = 0x01;
		l = 0x4D;
		sp = 0xFFFE;
		pc = 0x0100;
		
		ime = false;
		halted = false;
		
		cycles = 0;
	}

	public final void emulate(int ticks)
	{
		cycles += ticks;

		interrupt();

		while (cycles > 0)
			execute();
	}
	
	/*
	 * Interrupts
	 */
	public final void interrupt()
	{
		if (halted) {
			if (interrupt.isPending()) {
				halted = false;
				// Zerd no Densetsu
				cycles -= 4;
			}
			else {
				if (cycles > 0)
					cycles = 0;
			}
		}
		
		if (ime) {
			if (interrupt.isPending()) {
				if (interrupt.isPending(Interrupt.VBLANK)) {
					interrupt(0x40);
					interrupt.lower(Interrupt.VBLANK);
				}
				else if (interrupt.isPending(Interrupt.LCD)) {
					interrupt(0x48);
					interrupt.lower(Interrupt.LCD);
				}
				else if (interrupt.isPending(Interrupt.TIMER)) {
					interrupt(0x50);
					interrupt.lower(Interrupt.TIMER);
				}
				else if (interrupt.isPending(Interrupt.SERIAL)) {
					interrupt(0x58);
					interrupt.lower(Interrupt.SERIAL);
				}
				else if (interrupt.isPending(Interrupt.JOYPAD)) {
					interrupt(0x60);
					interrupt.lower(Interrupt.JOYPAD);
				}
			}
		}
	}
	
	private final void interrupt(int address)
	{
		ime = false;
		
		call(address);
	}

	/*
	 * Execution
	 */
	private final void execute()
	{		
		execute(fetch());
	}
	
	private final void execute(int opcode)
	{
		switch (opcode) {
		// NOP
		case 0x00:
			nop();
			break;
			
		// LD (nnnn),SP
		case 0x08:
			load_mem_SP();
			break;

		// STOP
		case 0x10:
			stop();
			break;

		// JR nn
		case 0x18:
			jr_nn();
			break;
			
		// JR cc,nn
		case 0x20:
			jr_NZ_nn();
			break;
		case 0x28:
			jr_Z_nn();
			break;
		case 0x30:
			jr_NC_nn();
			break;
		case 0x38:
			jr_C_nn();
			break;
		
		// LD rr,nnnn
		case 0x01:
			ld_BC_nnnn();
			break;
		case 0x11:
			ld_DE_nnnn();
			break;
		case 0x21:
			ld_HL_nnnn();
			break;
		case 0x31:
			ld_SP_nnnn();
			break;
			
		// ADD HL,rr
		case 0x09:
			add_HL_BC();
			break;
		case 0x19:
			add_HL_DE();
			break;
		case 0x29:
			add_HL_HL();
			break;
		case 0x39:
			add_HL_SP();
			break;

		// LD (BC),A
		case 0x02:
			ld_BCi_A();
			break;

		// LD A,(BC)
		case 0x0A:
			ld_A_BCi();
			break;

		// LD (DE),A
		case 0x12:
			ld_DEi_A();
			break;

		// LD A,(DE)
		case 0x1A:
			load_A_DEi();
			break;

		// LDI (HL),A
		case 0x22:
			ldi_HLi_A();
			break;

		// LDI A,(HL)
		case 0x2A:
			ldi_A_HLi();
			break;

		// LDD (HL),A
		case 0x32:
			ldd_HLi_A();
			break;

		// LDD A,(HL)
		case 0x3A:
			ldd_A_HLi();
			break;

		// INC rr
		case 0x03:
			inc_BC();
			break;
		case 0x13:
			inc_DE();
			break;
		case 0x23:
			inc_HL();
			break;
		case 0x33:
			inc_SP();
			break;

		// DEC rr
		case 0x0B:
			dec_BC();
			break;
		case 0x1B:
			dec_DE();
			break;
		case 0x2B:
			dec_HL();
			break;
		case 0x3B:
			dec_SP();
			break;
			
		// INC r
		case 0x04:
			inc_B();
			break;
		case 0x0C:
			inc_C();
			break;
		case 0x14:
			inc_D();
			break;
		case 0x1C:
			inc_E();
			break;
		case 0x24:
			inc_H();
			break;
		case 0x2C:
			inc_L();
			break;
		case 0x34:
			inc_HLi();
			break;
		case 0x3C:
			inc_A();
			break;
		
		// DEC r
		case 0x05:
			dec_B();
			break;
		case 0x0D:
			dec_C();
			break;
		case 0x15:
			dec_D();
			break;
		case 0x1D:
			dec_E();
			break;
		case 0x25:
			dec_H();
			break;
		case 0x2D:
			dec_L();
			break;
		case 0x35:
			dec_HLi();
			break;
		case 0x3D:
			dec_A();
			break;

		// LD r,nn
		case 0x06:
			ld_B_nn();
			break;
		case 0x0E:
			ld_C_nn();
			break;
		case 0x16:
			ld_D_nn();
			break;
		case 0x1E:
			ld_E_nn();
			break;
		case 0x26:
			ld_H_nn();
			break;
		case 0x2E:
			ld_L_nn();
			break;
		case 0x36:
			ld_HLi_nn();
			break;
		case 0x3E:
			ld_A_nn();
			break;
		
		// RLCA
		case 0x07:
			rlca();
			break;

		// RRCA
		case 0x0F:
			rrca();
			break;
		
		// RLA
		case 0x17:
			rla();
			break;

		// RRA
		case 0x1F:
			rra();
			break;
		
		// DAA
		case 0x27:
			daa();
			break;

		// CPL
		case 0x2F:
			cpl();
			break;

		// SCF
		case 0x37:
			scf();
			break;
		
		// CCF
		case 0x3F:
			ccf();
			break;
			
		// HALT
		case 0x76:
			halt();
			break;

		// LD r,s
		case 0x40:
			ld_B_B();
			break;
		case 0x41:
			ld_B_C();
			break;
		case 0x42:
			ld_B_D();
			break;
		case 0x43:
			ld_B_E();
			break;
		case 0x44:
			ld_B_H();
			break;
		case 0x45:
			ld_B_L();
			break;
		case 0x46:
			ld_B_HLi();
			break;
		case 0x47:
			ld_B_A();
			break;

		case 0x48:
			ld_C_B();
			break;
		case 0x49:
			ld_C_C();
			break;
		case 0x4A:
			ld_C_D();
			break;
		case 0x4B:
			ld_C_E();
			break;
		case 0x4C:
			ld_C_H();
			break;
		case 0x4D:
			ld_C_L();
			break;
		case 0x4E:
			ld_C_HLi();
			break;
		case 0x4F:
			ld_C_A();
			break;

		case 0x50:
			ld_D_B();
			break;
		case 0x51:
			ld_D_C();
			break;
		case 0x52:
			ld_D_D();
			break;
		case 0x53:
			ld_D_E();
			break;
		case 0x54:
			ld_D_H();
			break;
		case 0x55:
			ld_D_L();
			break;
		case 0x56:
			ld_D_HLi();
			break;
		case 0x57:
			ld_D_A();
			break;

		case 0x58:
			ld_E_B();
			break;
		case 0x59:
			ld_E_C();
			break;
		case 0x5A:
			ld_E_D();
			break;
		case 0x5B:
			ld_E_E();
			break;
		case 0x5C:
			ld_E_H();
			break;
		case 0x5D:
			ld_E_L();
			break;
		case 0x5E:
			ld_E_HLi();
			break;
		case 0x5F:
			ld_E_A();
			break;

		case 0x60:
			ld_H_B();
			break;
		case 0x61:
			ld_H_C();
			break;
		case 0x62:
			ld_H_D();
			break;
		case 0x63:
			ld_H_E();
			break;
		case 0x64:
			ld_H_H();
			break;
		case 0x65:
			ld_H_L();
			break;
		case 0x66:
			ld_H_HLi();
			break;
		case 0x67:
			ld_H_A();
			break;

		case 0x68:
			ld_L_B();
			break;
		case 0x69:
			ld_L_C();
			break;
		case 0x6A:
			ld_L_D();
			break;
		case 0x6B:
			ld_L_E();
			break;
		case 0x6C:
			ld_L_H();
			break;
		case 0x6D:
			ld_L_L();
			break;
		case 0x6E:
			ld_L_HLi();
			break;
		case 0x6F:
			ld_L_A();
			break;

		case 0x70:
			ld_HLi_B();
			break;
		case 0x71:
			ld_HLi_C();
			break;
		case 0x72:
			ld_HLi_D();
			break;
		case 0x73:
			ld_HLi_E();
			break;
		case 0x74:
			ld_HLi_H();
			break;
		case 0x75:
			ld_HLi_L();
			break;
		case 0x77:
			ld_HLi_A();
			break;

		case 0x78:
			ld_A_B();
			break;
		case 0x79:
			ld_A_C();
			break;
		case 0x7A:
			ld_A_D();
			break;
		case 0x7B:
			ld_A_E();
			break;
		case 0x7C:
			ld_A_H();
			break;
		case 0x7D:
			ld_A_L();
			break;
		case 0x7E:
			ld_A_HLi();
			break;
		case 0x7F:
			ld_A_A();
			break;
			
		// ADD A,r			
		case 0x80:
			add_A_B();
			break;
		case 0x81:
			add_A_C();
			break;
		case 0x82:
			add_A_D();
			break;
		case 0x83:
			add_A_E();
			break;
		case 0x84:
			add_A_H();
			break;
		case 0x85:
			add_A_L();
			break;
		case 0x86:
			add_A_HLi();
			break;
		case 0x87:
			add_A_A();
			break;

		// ADC A,r
		case 0x88:
			adc_A_B();
			break;
		case 0x89:
			adc_A_C();
			break;
		case 0x8A:
			adc_A_D();
			break;
		case 0x8B:
			adc_A_E();
			break;
		case 0x8C:
			adc_A_H();
			break;
		case 0x8D:
			adc_A_L();
			break;
		case 0x8E:
			adc_A_HLi();
			break;
		case 0x8F:
			adc_A_A();
			break;

		// SUB A,r
		case 0x90:
			sub_A_B();
			break;
		case 0x91:
			sub_A_C();
			break;
		case 0x92:
			sub_A_D();
			break;
		case 0x93:
			sub_A_E();
			break;
		case 0x94:
			sub_A_H();
			break;
		case 0x95:
			sub_A_L();
			break;
		case 0x96:
			sub_A_HLi();
			break;
		case 0x97:
			sub_A_A();
			break;

		// SBC A,r
		case 0x98:
			sbc_A_B();
			break;
		case 0x99:
			sbc_A_C();
			break;
		case 0x9A:
			sbc_A_D();
			break;
		case 0x9B:
			sbc_A_E();
			break;
		case 0x9C:
			sbc_A_H();
			break;
		case 0x9D:
			sbc_A_L();
			break;
		case 0x9E:
			sbc_A_HLi();
			break;
		case 0x9F:
			sbc_A_A();
			break;

		// AND A,r
		case 0xA0:
			and_A_B();
			break;
		case 0xA1:
			and_A_C();
			break;
		case 0xA2:
			and_A_D();
			break;
		case 0xA3:
			and_A_E();
			break;
		case 0xA4:
			and_A_H();
			break;
		case 0xA5:
			and_A_L();
			break;
		case 0xA6:
			and_A_HLi();
			break;
		case 0xA7:
			and_A_A();
			break;
			
		// XOR A,r
		case 0xA8:
			xor_A_B();
			break;
		case 0xA9:
			xor_A_C();
			break;
		case 0xAA:
			xor_A_D();
			break;
		case 0xAB:
			xor_A_E();
			break;
		case 0xAC:
			xor_A_H();
			break;
		case 0xAD:
			xor_A_L();
			break;
		case 0xAE:
			xor_A_HLi();
			break;
		case 0xAF:
			xor_A_A();
			break;

		// OR A,r
		case 0xB0:
			or_A_B();
			break;
		case 0xB1:
			or_A_C();
			break;
		case 0xB2:
			or_A_D();
			break;
		case 0xB3:
			or_A_E();
			break;
		case 0xB4:
			or_A_H();
			break;
		case 0xB5:
			or_A_L();
			break;
		case 0xB6:
			or_A_HLi();
			break;
		case 0xB7:
			or_A_A();
			break;

		// CP A,r
		case 0xB8:
			cp_A_B();
			break;
		case 0xB9:
			cp_A_C();
			break;
		case 0xBA:
			cp_A_D();
			break;
		case 0xBB:
			cp_A_E();
			break;
		case 0xBC:
			cp_A_H();
			break;
		case 0xBD:
			cp_A_L();
			break;
		case 0xBE:
			cp_A_HLi();
			break;
		case 0xBF:
			cp_A_A();
			break;
			
		// RET cc
		case 0xC0:
			ret_NZ();
			break;
		case 0xC8:
			ret_Z();
			break;
		case 0xD0:
			ret_NC();
			break;
		case 0xD8:
			ret_C();
			break;

		// LDH (nn),A
		case 0xE0:
			ldh_mem_A();
			break;
			
		// ADD SP,nn
		case 0xE8:
			add_SP_nn();
			break;

		// LDH A,(nn)
		case 0xF0:
			ldh_A_mem();
			break;

		// LD HL,SP+nn
		case 0xF8:
			ld_HP_SP_nn();
			break;

		// POP rr
		case 0xC1:
			pop_BC();
			break;
		case 0xD1:
			pop_DE();
			break;
		case 0xE1:
			pop_HL();
			break;
		case 0xF1:
			pop_AF();
			break;

		// RET
		case 0xC9:
			ret();
			break;
				
		// RETI
		case 0xD9:
			reti();
			break;

		// LD PC,HL
		case 0xE9:
			ld_PC_HL();
			break;

		// LD SP,HL
		case 0xF9:
			ld_SP_HL();
			break;

		// JP cc,nnnn
		case 0xC2:
			jp_NZ_nnnn();
			break;
		case 0xCA:
			jp_Z_nnnn();
			break;
		case 0xD2:
			jp_NC_nnnn();
			break;
		case 0xDA:
			jp_C_nnnn();
			break;

		// LDH (C),A
		case 0xE2:
			ldh_Ci_A();
			break;

		// LD (nnnn),A
		case 0xEA:
			ld_mem_A();
			break;

		// LDH A,(C)
		case 0xF2:
			ldh_A_Ci();
			break;

		// LD A,(nnnn)
		case 0xFA:
			ld_A_mem();
			break;

		// JP nnnn
		case 0xC3:
			jp_nnnn();
			break;
			
		case 0xCB:
			switch (fetch()) {
			// RLC r
			case 0x00:
				rlc_B();
				break;
			case 0x01:
				rlc_C();
				break;
			case 0x02:
				rlc_D();
				break;
			case 0x03:
				rlc_E();
				break;
			case 0x04:
				rlc_H();
				break;
			case 0x05:
				rlc_L();
				break;
			case 0x06:
				rlc_HLi();
				break;
			case 0x07:
				rlc_A();
				break;

			// RRC r
			case 0x08:
				rrc_B();
				break;
			case 0x09:
				rrc_C();
				break;
			case 0x0A:
				rrc_D();
				break;
			case 0x0B:
				rrc_E();
				break;
			case 0x0C:
				rrc_H();
				break;
			case 0x0D:
				rrc_L();
				break;
			case 0x0E:
				rrc_HLi();
				break;
			case 0x0F:
				rrc_A();
				break;
				
			// RL r
			case 0x10:
				rl_B();
				break;
			case 0x11:
				rl_C();
				break;
			case 0x12:
				rl_D();
				break;
			case 0x13:
				rl_E();
				break;
			case 0x14:
				rl_H();
				break;
			case 0x15:
				rl_L();
				break;
			case 0x16:
				rl_HLi();
				break;
			case 0x17:
				rl_A();
				break;

			// RR r
			case 0x18:
				rr_B();
				break;
			case 0x19:
				rr_C();
				break;
			case 0x1A:
				rr_D();
				break;
			case 0x1B:
				rr_E();
				break;
			case 0x1C:
				rr_H();
				break;
			case 0x1D:
				rr_L();
				break;
			case 0x1E:
				rr_HLi();
				break;
			case 0x1F:
				rr_A();
				break;

			// SLA r
			case 0x20:
				sla_B();
				break;
			case 0x21:
				sla_C();
				break;
			case 0x22:
				sla_D();
				break;
			case 0x23:
				sla_E();
				break;
			case 0x24:
				sla_H();
				break;
			case 0x25:
				sla_L();
				break;
			case 0x26:
				sla_HLi();
				break;
			case 0x27:
				sla_A();
				break;

			// SRA r
			case 0x28:
				sra_B();
				break;
			case 0x29:
				sra_C();
				break;
			case 0x2A:
				sra_D();
				break;
			case 0x2B:
				sra_E();
				break;
			case 0x2C:
				sra_H();
				break;
			case 0x2D:
				sra_L();
				break;
			case 0x2E:
				sra_HLi();
				break;
			case 0x2F:
				sra_A();
				break;

			// SWAP r
			case 0x30:
				swap_B();
				break;
			case 0x31:
				swap_C();
				break;
			case 0x32:
				swap_D();
				break;
			case 0x33:
				swap_E();
				break;
			case 0x34:
				swap_H();
				break;
			case 0x35:
				swap_L();
				break;
			case 0x36:
				swap_HLi();
				break;
			case 0x37:
				swap_A();
				break;

			// SRL r
			case 0x38:
				srl_B();
				break;
			case 0x39:
				srl_C();
				break;
			case 0x3A:
				srl_D();
				break;
			case 0x3B:
				srl_E();
				break;
			case 0x3C:
				srl_H();
				break;
			case 0x3D:
				srl_L();
				break;
			case 0x3E:
				srl_HLi();
				break;
			case 0x3F:
				srl_A();
				break;

			// BIT 0,r
			case 0x40:
				bit_B(0);
				break;
			case 0x41:
				bit_C(0);
				break;
			case 0x42:
				bit_D(0);
				break;
			case 0x43:
				bit_E(0);
				break;
			case 0x44:
				bit_H(0);
				break;
			case 0x45:
				bit_L(0);
				break;
			case 0x46:
				bit_HLi(0);
				break;
			case 0x47:
				bit_A(0);
				break;
				
			// BIT 1,r
			case 0x48:
				bit_B(1);
				break;
			case 0x49:
				bit_C(1);
				break;
			case 0x4A:
				bit_D(1);
				break;
			case 0x4B:
				bit_E(1);
				break;
			case 0x4C:
				bit_H(1);
				break;
			case 0x4D:
				bit_L(1);
				break;
			case 0x4E:
				bit_HLi(1);
				break;
			case 0x4F:
				bit_A(1);
				break;

			// BIT 2,r
			case 0x50:
				bit_B(2);
				break;
			case 0x51:
				bit_C(2);
				break;
			case 0x52:
				bit_D(2);
				break;
			case 0x53:
				bit_E(2);
				break;
			case 0x54:
				bit_H(2);
				break;
			case 0x55:
				bit_L(2);
				break;
			case 0x56:
				bit_HLi(2);
				break;
			case 0x57:
				bit_A(2);
				break;

			// BIT 3,r
			case 0x58:
				bit_B(3);
				break;
			case 0x59:
				bit_C(3);
				break;
			case 0x5A:
				bit_D(3);
				break;
			case 0x5B:
				bit_E(3);
				break;
			case 0x5C:
				bit_H(3);
				break;
			case 0x5D:
				bit_L(3);
				break;
			case 0x5E:
				bit_HLi(3);
				break;
			case 0x5F:
				bit_A(3);
				break;

			// BIT 4,r
			case 0x60:
				bit_B(4);
				break;
			case 0x61:
				bit_C(4);
				break;
			case 0x62:
				bit_D(4);
				break;
			case 0x63:
				bit_E(4);
				break;
			case 0x64:
				bit_H(4);
				break;
			case 0x65:
				bit_L(4);
				break;
			case 0x66:
				bit_HLi(4);
				break;
			case 0x67:
				bit_A(4);
				break;

			// BIT 5,r
			case 0x68:
				bit_B(5);
				break;
			case 0x69:
				bit_C(5);
				break;
			case 0x6A:
				bit_D(5);
				break;
			case 0x6B:
				bit_E(5);
				break;
			case 0x6C:
				bit_H(5);
				break;
			case 0x6D:
				bit_L(5);
				break;
			case 0x6E:
				bit_HLi(5);
				break;
			case 0x6F:
				bit_A(5);
				break;

			// BIT 6,r
			case 0x70:
				bit_B(6);
				break;
			case 0x71:
				bit_C(6);
				break;
			case 0x72:
				bit_D(6);
				break;
			case 0x73:
				bit_E(6);
				break;
			case 0x74:
				bit_H(6);
				break;
			case 0x75:
				bit_L(6);
				break;
			case 0x76:
				bit_HLi(6);
				break;
			case 0x77:
				bit_A(6);
				break;

			// BIT 7,r
			case 0x78:
				bit_B(7);
				break;
			case 0x79:
				bit_C(7);
				break;
			case 0x7A:
				bit_D(7);
				break;
			case 0x7B:
				bit_E(7);
				break;
			case 0x7C:
				bit_H(7);
				break;
			case 0x7D:
				bit_L(7);
				break;
			case 0x7E:
				bit_HLi(7);
				break;
			case 0x7F:
				bit_A(7);
				break;


			// SET 0,r
			case 0xC0:
				set_B(0);
				break;
			case 0xC1:
				set_C(0);
				break;
			case 0xC2:
				set_D(0);
				break;
			case 0xC3:
				set_E(0);
				break;
			case 0xC4:
				set_H(0);
				break;
			case 0xC5:
				set_L(0);
				break;
			case 0xC6:
				set_HLi(0);
				break;
			case 0xC7:
				set_A(0);
				break;
			
			// SET 1,r
			case 0xC8:
				set_B(1);
				break;
			case 0xC9:
				set_C(1);
				break;
			case 0xCA:
				set_D(1);
				break;
			case 0xCB:
				set_E(1);
				break;
			case 0xCC:
				set_H(1);
				break;
			case 0xCD:
				set_L(1);
				break;
			case 0xCE:
				set_HLi(1);
				break;
			case 0xCF:
				set_A(1);
				break;

			// SET 2,r
			case 0xD0:
				set_B(2);
				break;
			case 0xD1:
				set_C(2);
				break;
			case 0xD2:
				set_D(2);
				break;
			case 0xD3:
				set_E(2);
				break;
			case 0xD4:
				set_H(2);
				break;
			case 0xD5:
				set_L(2);
				break;
			case 0xD6:
				set_HLi(2);
				break;
			case 0xD7:
				set_A(2);
				break;

			// SET 3,r
			case 0xD8:
				set_B(3);
				break;	
			case 0xD9:
				set_C(3);
				break;				
			case 0xDA:
				set_D(3);
				break;
			case 0xDB:
				set_E(3);
				break;				
			case 0xDC:
				set_H(3);
				break;		
			case 0xDD:
				set_L(3);
				break;
			case 0xDE:
				set_HLi(3);
				break;
			case 0xDF:
				set_A(3);
				break;
				
			// SET 4,r
			case 0xE0:
				set_B(4);
				break;
			case 0xE1:
				set_C(4);
				break;
			case 0xE2:
				set_D(4);
				break;
			case 0xE3:
				set_E(4);
				break;
			case 0xE4:
				set_H(4);
				break;
			case 0xE5:
				set_L(4);
				break;
			case 0xE6:
				set_HLi(4);
				break;
			case 0xE7:
				set_A(4);
				break;

			// SET 5,r
			case 0xE8:
				set_B(5);
				break;
			case 0xE9:
				set_C(5);
				break;
			case 0xEA:
				set_D(5);
				break;
			case 0xEB:
				set_E(5);
				break;
			case 0xEC:
				set_H(5);
				break;
			case 0xED:
				set_L(5);
				break;
			case 0xEE:
				set_HLi(5);
				break;
			case 0xEF:
				set_A(5);
				break;
			
			// SET 6,r
			case 0xF0:
				set_B(6);
				break;
			case 0xF1:
				set_C(6);
				break;
			case 0xF2:
				set_D(6);
				break;
			case 0xF3:
				set_E(6);
				break;
			case 0xF4:
				set_H(6);
				break;
			case 0xF5:
				set_L(6);
				break;
			case 0xF6:
				set_HLi(6);
				break;
			case 0xF7:
				set_A(6);
				break;

			// SET 7,r
			case 0xF8:
				set_B(7);
				break;
			case 0xF9:
				set_C(7);
				break;
			case 0xFA:
				set_D(7);
				break;
			case 0xFB:
				set_E(7);
				break;
			case 0xFC:
				set_H(7);
				break;
			case 0xFD:
				set_L(7);
				break;
			case 0xFE:
				set_HLi(7);
				break;
			case 0xFF:
				set_A(7);
				break;

			// RES 0,r
			case 0x80:
				res_B(0);
				break;
			case 0x81:
				res_C(0);
				break;
			case 0x82:
				res_D(0);
				break;
			case 0x83:
				res_E(0);
				break;
			case 0x84:
				res_H(0);
				break;
			case 0x85:
				res_L(0);
				break;
			case 0x86:
				res_HLi(0);
				break;
			case 0x87:
				res_A(0);
				break;
			
			// RES 1,r
			case 0x88:
				res_B(1);
				break;
			case 0x89:
				res_C(1);
				break;
			case 0x8A:
				res_D(1);
				break;
			case 0x8B:
				res_E(1);
				break;
			case 0x8C:
				res_H(1);
				break;
			case 0x8D:
				res_L(1);
				break;
			case 0x8E:
				res_HLi(1);
				break;
			case 0x8F:
				res_A(1);
				break;
				
			// RES 2,r
			case 0x90:
				res_B(2);
				break;
			case 0x91:
				res_C(2);
				break;
			case 0x92:
				res_D(2);
				break;
			case 0x93:
				res_E(2);
				break;
			case 0x94:
				res_H(2);
				break;
			case 0x95:
				res_L(2);
				break;
			case 0x96:
				res_HLi(2);
				break;
			case 0x97:
				res_A(2);
				break;
				
			// RES 3,r
			case 0x98:
				res_B(3);
				break;
			case 0x99:
				res_C(3);
				break;
			case 0x9A:
				res_D(3);
				break;
			case 0x9B:
				res_E(3);
				break;
			case 0x9C:
				res_H(3);
				break;
			case 0x9D:
				res_L(3);
				break;
			case 0x9E:
				res_HLi(3);
				break;
			case 0x9F:
				res_A(3);
				break;
				
			// RES 4,r
			case 0xA0:
				res_B(4);
				break;
			case 0xA1:
				res_C(4);
				break;
			case 0xA2:
				res_D(4);
				break;
			case 0xA3:
				res_E(4);
				break;
			case 0xA4:
				res_H(4);
				break;
			case 0xA5:
				res_L(4);
				break;
			case 0xA6:
				res_HLi(4);
				break;
			case 0xA7:
				res_A(4);
				break;
				
			// RES 5,r
			case 0xA8:
				res_B(5);
				break;
			case 0xA9:
				res_C(5);
				break;
			case 0xAA:
				res_D(5);
				break;
			case 0xAB:
				res_E(5);
				break;
			case 0xAC:
				res_H(5);
				break;
			case 0xAD:
				res_L(5);
				break;
			case 0xAE:
				res_HLi(5);
				break;
			case 0xAF:
				res_A(5);
				break;

			// RES 6,r				
			case 0xB0:
				res_B(6);
				break;
			case 0xB1:
				res_C(6);
				break;
			case 0xB2:
				res_D(6);
				break;
			case 0xB3:
				res_E(6);
				break;
			case 0xB4:
				res_H(6);
				break;
			case 0xB5:
				res_L(6);
				break;
			case 0xB6:
				res_HLi(6);
				break;
			case 0xB7:
				res_A(6);
				break;

			// RES 7,r				
			case 0xB8:
				res_B(7);
				break;
			case 0xB9:
				res_C(7);
				break;
			case 0xBA:
				res_D(7);
				break;
			case 0xBB:
				res_E(7);
				break;
			case 0xBC:
				res_H(7);
				break;
			case 0xBD:
				res_L(7);
				break;
			case 0xBE:
				res_HLi(7);
				break;
			case 0xBF:
				res_A(7);
				break;
			}
			break;

		// DI
		case 0xF3:
			di();
			break;

		// EI
		case 0xFB:
			ei();
			break;

		// CALL cc,nnnn
		case 0xC4:
			call_NZ_nnnn();
			break;
		case 0xCC:
			call_Z_nnnn();
			break;
		case 0xD4:
			call_NC_nnnn();
			break;
		case 0xDC:
			call_C_nnnn();
			break;

		// PUSH rr
		case 0xC5:
			push_BC();
			break;
		case 0xD5:
			push_DE();
			break;
		case 0xE5:
			push_HL();
			break;
		case 0xF5:
			push_AF();
			break;

		// CALL nnnn
		case 0xCD:
			call_nnnn();
			break;
			
		// ADD A,nn
		case 0xC6:
			add_A_nn();
			break;

		// ADC A,nn
		case 0xCE:
			adc_A_nn();
			break;
		
		// SUB A,nn
		case 0xD6:
			sub_A_nn();
			break;

		// SBC A,nn
		case 0xDE:
			sbc_A_nn();
			break;
	
		// AND A,nn
		case 0xE6:
			and_A_nn();
			break;

		// XOR A,nn
		case 0xEE:
			xor_A_nn();
			break;

		// OR A,nn
		case 0xF6:
			or_A_nn();
			break;
			
		// CP A,nn
		case 0xFE:
			cp_A_nn();
			break;

		// RST nn
		case 0xC7:
			rst(0x00);
			break;
		case 0xCF:
			rst(0x08);
			break;
		case 0xD7:
			rst(0x10);
			break;
		case 0xDF:
			rst(0x18);
			break;
		case 0xE7:
			rst(0x20);
			break;
		case 0xEF:
			rst(0x28);
			break;
		case 0xF7:
			rst(0x30);
			break;
		case 0xFF:
			rst(0x38);
			break;

		default:
			throw new RuntimeException("Invalid operation");
		}
	}

	/*
	 * Memory Access
	 */
	private final int read(int address)
	{
		return memory.read(address);
	}

	private final void write(int address, int data)
	{
		memory.write(address, data);
	}

	private final int read(int hi, int lo)
	{
		return read((hi << 8) + lo);
	}

	private final void write(int hi, int lo, int data)
	{
		write((hi << 8) + lo, data);
	}

	/*
	 * Fetching
	 */
	private final int fetch()
	{
		if (pc <= 0x3FFF)
			return rom[pc++] & 0xFF;
		
		int data = memory.read(pc);
		pc = (pc + 1) & 0xFFFF;
		return data;
	}

	/*
	 * Stack
	 */
	private final void push(int data)
	{
		sp = (sp - 1) & 0xFFFF;
		memory.write(sp, data);
	}

	private final int pop()
	{
		int data = memory.read(sp);
		sp = (sp + 1) & 0xFFFF;
		return data;
	}

	private final void call(int address)
	{
		push(pc >> 8);
		push(pc & 0xFF);
		pc = address;
	}

	/*
	 * ALU
	 */
	private final void add(int data)
	{
		int s = (a + data) & 0xFF;
		f = (s == 0 ? Z_FLAG : 0) + (s < a ? C_FLAG : 0) + ((s & 0x0F) < (a & 0x0F) ? H_FLAG : 0);
		a = s;
	}

	private final void adc(int data)
	{
		int s = a + data + ((f & C_FLAG) >> 4);
		f = ((s & 0xff) == 0 ? Z_FLAG : 0) + (s >= 0x100 ? C_FLAG : 0) + (((s ^ a ^ data) & 0x10) != 0 ? H_FLAG : 0);
		a = s & 0xFF;
	}

	private final void sub(int data)
	{
		int s = (a - data) & 0xFF;
		f = (s == 0 ? Z_FLAG : 0) + (s > a ? C_FLAG : 0) + ((s & 0x0F) > (a & 0x0F) ? H_FLAG : 0) + N_FLAG;
		a = s;
	}

	private final void sbc(int data)
	{
		int s = a - data - ((f & C_FLAG) >> 4);
		f = ((s & 0xFF) == 0 ? Z_FLAG : 0) + ((s & 0xFF00) != 0 ? C_FLAG : 0) + (((s ^ a ^ data) & 0x10) != 0 ? H_FLAG : 0) + N_FLAG;
		a = s & 0xFF;
	}

	private final void and(int data)
	{
		a &= data;
		f = (a == 0 ? Z_FLAG : 0);
	}

	private final void xor(int data)
	{
		a ^= data;
		f = (a == 0 ? Z_FLAG : 0);
	}

	private final void or(int data)
	{
		a |= data;
		f = (a == 0 ? Z_FLAG : 0);
	}

	private final void cp(int data)
	{
		int s = (a - data) & 0xFF;
		f = (s == 0 ? Z_FLAG : 0) + (s > a ? C_FLAG : 0) + ((s & 0x0F) > (a & 0x0F) ? H_FLAG : 0) + N_FLAG;
	}

	private final int inc(int data)
	{
		data = (data + 1) & 0xFF;
		f = (data == 0 ? Z_FLAG : 0) + ((data & 0x0F) == 0x00 ? H_FLAG : 0) + (f & C_FLAG);
		return data;
	}

	private final int dec(int data)
	{
		data = (data - 1) & 0xFF;
		f = (data == 0 ? Z_FLAG : 0) + ((data & 0x0F) == 0x0F ? H_FLAG : 0) + (f & C_FLAG) + N_FLAG;
		return data;
	}

	private final int rlc(int data)
	{
		int s = ((data & 0x7F) << 1) + ((data & 0x80) >> 7);
		f = (s == 0 ? Z_FLAG : 0) + ((data & 0x80) != 0 ? C_FLAG : 0);
		return s;
	}

	private final int rl(int data)
	{
		int s = ((data & 0x7F) << 1) + ((f & C_FLAG) != 0 ? 0x01 : 0x00);
		f = (s == 0 ? Z_FLAG : 0) + ((data & 0x80) != 0 ? C_FLAG : 0);
		return s;
	}

	private final int rrc(int data)
	{
		int s = (data >> 1) + ((data & 0x01) << 7);
		f = (s == 0 ? Z_FLAG : 0) + ((data & 0x01) != 0 ? C_FLAG : 0);
		return s;
	}

	private final int rr(int data)
	{
		int s = (data >> 1) + ((f & C_FLAG) << 3);
		f = (s == 0 ? Z_FLAG : 0) + ((data & 0x01) != 0 ? C_FLAG : 0);
		return s;
	}

	private final int sla(int data)
	{
		int s = (data << 1) & 0xFF;
		f = (s == 0 ? Z_FLAG : 0) + ((data & 0x80) != 0 ? C_FLAG : 0);
		return s;
	}

	private final int sra(int data)
	{
		int s = (data >> 1) + (data & 0x80);
		f = (s == 0 ? Z_FLAG : 0) + ((data & 0x01) != 0 ? C_FLAG : 0);
		return s;
	}

	private final int srl(int data)
	{
		int s = (data >> 1);
		f = (s == 0 ? Z_FLAG : 0) + ((data & 0x01) != 0 ? C_FLAG : 0);
		return s;
	}

	private final int swap(int data)
	{
		int s = ((data << 4) & 0xF0) + ((data >> 4) & 0x0F);
		f = (s == 0 ? Z_FLAG : 0);
		return s;
	}

	private final void bit(int n, int data)
	{
		f = (f & C_FLAG) + H_FLAG + ((data & (1 << n)) == 0 ? Z_FLAG : 0);
	}

	private final void add(int hi, int lo)
	{
		int s = ((h << 8) + l + (hi << 8) + lo) & 0xFFFF;
		f = (f & Z_FLAG) + (((s >> 8) & 0x0F) < (h & 0x0F) ? H_FLAG : 0) + (s < (h << 8) + l ? C_FLAG : 0);
		l = s & 0xFF;
		h = s >> 8;
	}

	/*
	 * LD r,r
	 */
	private final void ld_B_B()
	{
		// b = b;
		cycles -= 1;
	}

	private final void ld_B_C()
	{
		b = c;
		cycles -= 1;
	}

	private final void ld_B_D()
	{
		b = d;
		cycles -= 1;
	}

	private final void ld_B_E()
	{
		b = e;
		cycles -= 1;
	}

	private final void ld_B_H()
	{
		b = h;
		cycles -= 1;
	}

	private final void ld_B_L()
	{
		b = l;
		cycles -= 1;
	}

	private final void ld_B_A()
	{
		b = a;
		cycles -= 1;
	}

	private final void ld_C_B()
	{
		c = b;
		cycles -= 1;
	}

	private final void ld_C_C()
	{
		// c = c;
		cycles -= 1;
	}

	private final void ld_C_D()
	{
		c = d;
		cycles -= 1;
	}

	private final void ld_C_E()
	{
		c = e;
		cycles -= 1;
	}

	private final void ld_C_H()
	{
		c = h;
		cycles -= 1;
	}

	private final void ld_C_L()
	{
		c = l;
		cycles -= 1;
	}

	private final void ld_C_A()
	{
		c = a;
		cycles -= 1;
	}

	private final void ld_D_B()
	{
		d = b;
		cycles -= 1;
	}

	private final void ld_D_C()
	{
		d = c;
		cycles -= 1;
	}

	private final void ld_D_D()
	{
		// d = d;
		cycles -= 1;
	}

	private final void ld_D_E()
	{
		d = e;
		cycles -= 1;
	}

	private final void ld_D_H()
	{
		d = h;
		cycles -= 1;
	}

	private final void ld_D_L()
	{
		d = l;
		cycles -= 1;
	}

	private final void ld_D_A()
	{
		d = a;
		cycles -= 1;
	}

	private final void ld_E_B()
	{
		e = b;
		cycles -= 1;
	}

	private final void ld_E_C()
	{
		e = c;
		cycles -= 1;
	}

	private final void ld_E_D()
	{
		e = d;
		cycles -= 1;
	}

	private final void ld_E_E()
	{
		// e = e;
		cycles -= 1;
	}

	private final void ld_E_H()
	{
		e = h;
		cycles -= 1;
	}

	private final void ld_E_L()
	{
		e = l;
		cycles -= 1;
	}

	private final void ld_E_A()
	{
		e = a;
		cycles -= 1;
	}

	private final void ld_H_B()
	{
		h = b;
		cycles -= 1;
	}

	private final void ld_H_C()
	{
		h = c;
		cycles -= 1;
	}

	private final void ld_H_D()
	{
		h = d;
		cycles -= 1;
	}

	private final void ld_H_E()
	{
		h = e;
		cycles -= 1;
	}

	private final void ld_H_H()
	{
		// h = h;
		cycles -= 1;
	}

	private final void ld_H_L()
	{
		h = l;
		cycles -= 1;
	}

	private final void ld_H_A()
	{
		h = a;
		cycles -= 1;
	}

	private final void ld_L_B()
	{
		l = b;
		cycles -= 1;
	}

	private final void ld_L_C()
	{
		l = c;
		cycles -= 1;
	}

	private final void ld_L_D()
	{
		l = d;
		cycles -= 1;
	}

	private final void ld_L_E()
	{
		l = e;
		cycles -= 1;
	}

	private final void ld_L_H()
	{
		l = h;
		cycles -= 1;
	}

	private final void ld_L_L()
	{
		// l = l;
		cycles -= 1;
	}

	private final void ld_L_A()
	{
		l = a;
		cycles -= 1;
	}

	private final void ld_A_B()
	{
		a = b;
		cycles -= 1;
	}

	private final void ld_A_C()
	{
		a = c;
		cycles -= 1;
	}

	private final void ld_A_D()
	{
		a = d;
		cycles -= 1;
	}

	private final void ld_A_E()
	{
		a = e;
		cycles -= 1;
	}

	private final void ld_A_H()
	{
		a = h;
		cycles -= 1;
	}

	private final void ld_A_L()
	{
		a = l;
		cycles -= 1;
	}

	private final void ld_A_A()
	{
		// a = a;
		cycles -= 1;
	}

	/*
	 * LD r,nn
	 */
	private final void ld_B_nn()
	{
		b = fetch();
		cycles -= 2;
	}

	private final void ld_C_nn()
	{
		c = fetch();
		cycles -= 2;
	}

	private final void ld_D_nn()
	{
		d = fetch();
		cycles -= 2;
	}

	private final void ld_E_nn()
	{
		e = fetch();
		cycles -= 2;
	}

	private final void ld_H_nn()
	{
		h = fetch();
		cycles -= 2;
	}

	private final void ld_L_nn()
	{
		l = fetch();
		cycles -= 2;
	}

	private final void ld_A_nn()
	{
		a = fetch();
		cycles -= 2;
	}

	/*
	 * LD r,(HL)
	 */
	private final void ld_B_HLi()
	{
		b = read(h, l);
		cycles -= 2;
	}

	private final void ld_C_HLi()
	{
		c = read(h, l);
		cycles -= 2;
	}

	private final void ld_D_HLi()
	{
		d = read(h, l);
		cycles -= 2;
	}

	private final void ld_E_HLi()
	{
		e = read(h, l);
		cycles -= 2;
	}

	private final void ld_H_HLi()
	{
		h = read(h, l);
		cycles -= 2;
	}

	private final void ld_L_HLi()
	{
		l = read(h, l);
		cycles -= 2;
	}

	private final void ld_A_HLi()
	{
		a = read(h, l);
		cycles -= 2;
	}

	/*
	 * LD (HL),r
	 */
	private final void ld_HLi_B()
	{
		write(h, l, b);
		cycles -= 2;
	}

	private final void ld_HLi_C()
	{
		write(h, l, c);
		cycles -= 2;
	}

	private final void ld_HLi_D()
	{
		write(h, l, d);
		cycles -= 2;
	}

	private final void ld_HLi_E()
	{
		write(h, l, e);
		cycles -= 2;
	}

	private final void ld_HLi_H()
	{
		write(h, l, h);
		cycles -= 2;
	}

	private final void ld_HLi_L()
	{
		write(h, l, l);
		cycles -= 2;
	}

	private final void ld_HLi_A()
	{
		write(h, l, a);
		cycles -= 2;
	}

	/*
	 * LD (HL),nn
	 */
	private final void ld_HLi_nn()
	{
		write(h, l, fetch());
		cycles -= 3;
	}

	/*
	 * LD A,(rr)
	 */
	private final void ld_A_BCi()
	{
		a = read(b, c);
		cycles -= 2;
	}

	private final void load_A_DEi()
	{
		a = read(d, e);
		cycles -= 2;
	}

	/*
	 * LD A,(nnnn)
	 */
	private final void ld_A_mem()
	{
		int lo = fetch();
		int hi = fetch();
		a = read(hi, lo);
		cycles -= 4;
	}

	/*
	 * LD (rr),A
	 */
	private final void ld_BCi_A()
	{
		write(b, c, a);
		cycles -= 2;
	}

	private final void ld_DEi_A()
	{
		write(d, e, a);
		cycles -= 2;
	}

	/*
	 * LD (nnnn),SP
	 */
	private final void load_mem_SP()
	{
		int lo = fetch();
		int hi = fetch();
		int address = (hi << 8) + lo;
		
		write(address, sp & 0xFF);
		write((address + 1) & 0xFFFF, sp >> 8);

		cycles -= 5;
	}
	
	/*
	 * LD (nnnn),A
	 */
	private final void ld_mem_A()
	{
		int lo = fetch();
		int hi = fetch();
		write(hi, lo, a);
		cycles -= 4;
	}

	/*
	 * LDH A,(nn)
	 */
	private final void ldh_A_mem()
	{
		a = read(0xFF00 + fetch());
		cycles -= 3;
	}

	/*
	 * LDH (nn),A
	 */
	private final void ldh_mem_A()
	{
		write(0xFF00 + fetch(), a);
		cycles -= 3;
	}

	/*
	 * LDH A,(C)
	 */
	private final void ldh_A_Ci()
	{
		a = read(0xFF00 + c);
		cycles -= 2;
	}

	/*
	 * LDH (C),A
	 */
	private final void ldh_Ci_A()
	{
		write(0xFF00 + c, a);
		cycles -= 2;
	}

	/*
	 * LDI (HL),A
	 */
	private final void ldi_HLi_A()
	{
		write(h, l, a);
		l = (l + 1) & 0xFF;
		if (l == 0)
			h = (h + 1) & 0xFF;
		cycles -= 2;
	}

	/*
	 * LDI A,(HL)
	 */
	private final void ldi_A_HLi()
	{
		a = read(h, l);
		l = (l + 1) & 0xFF;
		if (l == 0)
			h = (h + 1) & 0xFF;
		cycles -= 2;
	}

	/*
	 * LDD (HL),A
	 */
	private final void ldd_HLi_A()
	{
		write(h, l, a);
		l = (l - 1) & 0xFF;
		if (l == 0xFF)
			h = (h - 1) & 0xFF;
		cycles -= 2;
	}

	/*
	 * LDD A,(HL)
	 */
	private final void ldd_A_HLi()
	{
		a = read(h, l);
		l = (l - 1) & 0xFF;
		if (l == 0xFF)
			h = (h - 1) & 0xFF;
		cycles -= 2;
	}

	/*
	 * LD rr,nnnn
	 */
	private final void ld_BC_nnnn()
	{
		c = fetch();
		b = fetch();
		cycles -= 3;
	}

	private final void ld_DE_nnnn()
	{
		e = fetch();
		d = fetch();
		cycles -= 3;
	}

	private final void ld_HL_nnnn()
	{
		l = fetch();
		h = fetch();
		cycles -= 3;
	}

	private final void ld_SP_nnnn()
	{
		int lo = fetch();
		int hi = fetch();
		sp = (hi << 8) + lo;
		cycles -= 3;
	}

	/*
	 * LD SP,HL
	 */
	private final void ld_SP_HL()
	{
		sp = (h << 8) + l;
		cycles -= 2;
	}

	/*
	 * PUSH rr
	 */
	private final void push_BC()
	{
		push(b);
		push(c);
		cycles -= 4;
	}

	private final void push_DE()
	{
		push(d);
		push(e);
		cycles -= 4;
	}

	private final void push_HL()
	{
		push(h);
		push(l);
		cycles -= 4;
	}

	private final void push_AF()
	{
		push(a);
		push(f);
		cycles -= 4;
	}

	/*
	 * POP rr
	 */
	private final void pop_BC()
	{
		c = pop();
		b = pop();
		cycles -= 3;
	}

	private final void pop_DE()
	{
		e = pop();
		d = pop();
		cycles -= 3;
	}

	private final void pop_HL()
	{
		l = pop();
		h = pop();
		cycles -= 3;
	}

	private final void pop_AF()
	{
		f = pop();
		a = pop();
		cycles -= 3;
	}

	/*
	 * ADD A,r
	 */
	private final void add_A_B()
	{
		add(b);
		cycles -= 1;
	}

	private final void add_A_C()
	{
		add(c);
		cycles -= 1;
	}

	private final void add_A_D()
	{
		add(d);
		cycles -= 1;
	}

	private final void add_A_E()
	{
		add(e);
		cycles -= 1;
	}

	private final void add_A_H()
	{
		add(h);
		cycles -= 1;
	}

	private final void add_A_L()
	{
		add(l);
		cycles -= 1;
	}

	private final void add_A_A()
	{
		add(a);
		cycles -= 1;
	}

	/*
	 * ADD A,nn
	 */
	private final void add_A_nn()
	{
		add(fetch());
		cycles -= 2;
	}

	/*
	 * ADD A,(HL)
	 */
	private final void add_A_HLi()
	{
		add(read(h, l));
		cycles -= 2;
	}

	/*
	 * ADC A,r
	 */
	private final void adc_A_B()
	{
		adc(b);
		cycles -= 1;
	}

	private final void adc_A_C()
	{
		adc(c);
		cycles -= 1;
	}

	private final void adc_A_D()
	{
		adc(d);
		cycles -= 1;
	}

	private final void adc_A_E()
	{
		adc(e);
		cycles -= 1;
	}

	private final void adc_A_H()
	{
		adc(h);
		cycles -= 1;
	}

	private final void adc_A_L()
	{
		adc(l);
		cycles -= 1;
	}

	private final void adc_A_A()
	{
		adc(a);
		cycles -= 1;
	}

	/*
	 * ADC A,nn
	 */
	private final void adc_A_nn()
	{
		adc(fetch());
		cycles -= 2;
	}

	/*
	 * ADC A,(HL)
	 */
	private final void adc_A_HLi()
	{
		adc(read(h, l));
		cycles -= 2;
	}

	/*
	 * SUB A,r
	 */
	private final void sub_A_B()
	{
		sub(b);
		cycles -= 1;
	}

	private final void sub_A_C()
	{
		sub(c);
		cycles -= 1;
	}

	private final void sub_A_D()
	{
		sub(d);
		cycles -= 1;
	}

	private final void sub_A_E()
	{
		sub(e);
		cycles -= 1;
	}

	private final void sub_A_H()
	{
		sub(h);
		cycles -= 1;
	}

	private final void sub_A_L()
	{
		sub(l);
		cycles -= 1;
	}

	private final void sub_A_A()
	{
		sub(a);
		cycles -= 1;
	}

	/*
	 * SUB A,nn
	 */
	private final void sub_A_nn()
	{
		sub(fetch());
		cycles -= 2;
	}

	/*
	 * SUB A,(HL)
	 */
	private final void sub_A_HLi()
	{
		sub(read(h, l));
		cycles -= 2;
	}

	/*
	 * SBC A,r
	 */
	private final void sbc_A_B()
	{
		sbc(b);
		cycles -= 1;
	}

	private final void sbc_A_C()
	{
		sbc(c);
		cycles -= 1;
	}

	private final void sbc_A_D()
	{
		sbc(d);
		cycles -= 1;
	}

	private final void sbc_A_E()
	{
		sbc(e);
		cycles -= 1;
	}

	private final void sbc_A_H()
	{
		sbc(h);
		cycles -= 1;
	}

	private final void sbc_A_L()
	{
		sbc(l);
		cycles -= 1;
	}

	private final void sbc_A_A()
	{
		sbc(a);
		cycles -= 1;
	}

	/*
	 * SBC A,nn
	 */
	private final void sbc_A_nn()
	{
		sbc(fetch());
		cycles -= 2;
	}

	/*
	 * SBC A,(HL)
	 */
	private final void sbc_A_HLi()
	{
		sbc(read(h, l));
		cycles -= 2;
	}

	/*
	 * AND A,r
	 */
	private final void and_A_B()
	{
		and(b);
		cycles -= 1;
	}

	private final void and_A_C()
	{
		and(c);
		cycles -= 1;
	}

	private final void and_A_D()
	{
		and(d);
		cycles -= 1;
	}

	private final void and_A_E()
	{
		and(e);
		cycles -= 1;
	}

	private final void and_A_H()
	{
		and(h);
		cycles -= 1;
	}

	private final void and_A_L()
	{
		and(l);
		cycles -= 1;
	}

	private final void and_A_A()
	{
		and(a);
		cycles -= 1;
	}

	/*
	 * AND A,nn
	 */
	private final void and_A_nn()
	{
		and(fetch());
		cycles -= 2;
	}

	/*
	 * AND A,(HL)
	 */
	private final void and_A_HLi()
	{
		and(read(h, l));
		cycles -= 2;
	}

	/*
	 * XOR A,r
	 */
	private final void xor_A_B()
	{
		xor(b);
		cycles -= 1;
	}

	private final void xor_A_C()
	{
		xor(c);
		cycles -= 1;
	}

	private final void xor_A_D()
	{
		xor(d);
		cycles -= 1;
	}

	private final void xor_A_E()
	{
		xor(e);
		cycles -= 1;
	}

	private final void xor_A_H()
	{
		xor(h);
		cycles -= 1;
	}

	private final void xor_A_L()
	{
		xor(l);
		cycles -= 1;
	}

	private final void xor_A_A()
	{
		xor(a);
		cycles -= 1;
	}

	/*
	 * XOR A,nn
	 */
	private final void xor_A_nn()
	{
		xor(fetch());
		cycles -= 2;
	}

	/*
	 * XOR A,(HL)
	 */
	private final void xor_A_HLi()
	{
		xor(read(h, l));
		cycles -= 2;
	}

	/*
	 * OR A,r
	 */
	private final void or_A_B()
	{
		or(b);
		cycles -= 1;
	}

	private final void or_A_C()
	{
		or(c);
		cycles -= 1;
	}

	private final void or_A_D()
	{
		or(d);
		cycles -= 1;
	}

	private final void or_A_E()
	{
		or(e);
		cycles -= 1;
	}

	private final void or_A_H()
	{
		or(h);
		cycles -= 1;
	}

	private final void or_A_L()
	{
		or(l);
		cycles -= 1;
	}

	private final void or_A_A()
	{
		or(a);
		cycles -= 1;
	}

	/*
	 * OR A,nn
	 */
	private final void or_A_nn()
	{
		or(fetch());
		cycles -= 2;
	}

	/*
	 * OR A,(HL)
	 */
	private final void or_A_HLi()
	{
		or(read(h, l));
		cycles -= 2;
	}

	/*
	 * CP A,r
	 */
	private final void cp_A_B()
	{
		cp(b);
		cycles -= 1;
	}

	private final void cp_A_C()
	{
		cp(c);
		cycles -= 1;
	}

	private final void cp_A_D()
	{
		cp(d);
		cycles -= 1;
	}

	private final void cp_A_E()
	{
		cp(e);
		cycles -= 1;
	}

	private final void cp_A_H()
	{
		cp(h);
		cycles -= 1;
	}

	private final void cp_A_L()
	{
		cp(l);
		cycles -= 1;
	}

	private final void cp_A_A()
	{
		cp(a);
		cycles -= 1;
	}

	/*
	 * CP A,nn
	 */
	private final void cp_A_nn()
	{
		cp(fetch());
		cycles -= 2;
	}

	/*
	 * CP A,(HL)
	 */
	private final void cp_A_HLi()
	{
		cp(read(h, l));
		cycles -= 2;
	}

	/*
	 * INC r
	 */
	private final void inc_B()
	{
		b = inc(b);
		cycles -= 1;
	}

	private final void inc_C()
	{
		c = inc(c);
		cycles -= 1;
	}

	private final void inc_D()
	{
		d = inc(d);
		cycles -= 1;
	}

	private final void inc_E()
	{
		e = inc(e);
		cycles -= 1;
	}

	private final void inc_H()
	{
		h = inc(h);
		cycles -= 1;
	}

	private final void inc_L()
	{
		l = inc(l);
		cycles -= 1;
	}

	private final void inc_A()
	{
		a = inc(a);
		cycles -= 1;
	}

	/*
	 * INC (HL)
	 */
	private final void inc_HLi()
	{
		write(h, l, inc(read(h, l)));
		cycles -= 3;
	}

	/*
	 * DEC r
	 */
	private final void dec_B()
	{
		b = dec(b);
		cycles -= 1;
	}

	private final void dec_C()
	{
		c = dec(c);
		cycles -= 1;
	}

	private final void dec_D()
	{
		d = dec(d);
		cycles -= 1;
	}

	private final void dec_E()
	{
		e = dec(e);
		cycles -= 1;
	}

	private final void dec_H()
	{
		h = dec(h);
		cycles -= 1;
	}

	private final void dec_L()
	{
		l = dec(l);
		cycles -= 1;
	}

	private final void dec_A()
	{
		a = dec(a);
		cycles -= 1;
	}

	/*
	 * DEC (HL)
	 */
	private final void dec_HLi()
	{
		write(h, l, dec(read(h, l)));
		cycles -= 3;
	}

	/*
	 * CPL
	 */
	private final void cpl()
	{
		a ^= 0xFF;
		f |= N_FLAG + H_FLAG;
	}

	/*
	 * DAA
	 */
	private final void daa()
	{
		int delta = 0;

		if ((f & H_FLAG) != 0 || (a & 0x0F) > 0x09)
			delta |= 0x06;

		if ((f & C_FLAG) != 0 || (a & 0xF0) > 0x90)
			delta |= 0x60;

		if ((a & 0xF0) > 0x80 && (a & 0x0F) > 0x09)
			delta |= 0x60;

		if ((f & N_FLAG) == 0)
			a = (a + delta) & 0xFF;
		else
			a = (a - delta) & 0xFF;

		f = (f & N_FLAG) + (delta >= 0x60 ? C_FLAG : 0) + (a == 0 ? Z_FLAG : 0);

		cycles -= 1;
	}

	/*
	 * ADD HL,rr
	 */
	private final void add_HL_BC()
	{
		add(b, c);
		cycles -= 2;
	}

	private final void add_HL_DE()
	{
		add(d, e);
		cycles -= 2;
	}

	private final void add_HL_HL()
	{
		add(h, l);
		cycles -= 2;
	}

	private final void add_HL_SP()
	{
		add(sp >> 8, sp & 0xFF);
		cycles -= 2;
	}

	/*
	 * INC rr
	 */
	private final void inc_BC()
	{
		c = (c + 1) & 0xFF;
		if (c == 0x00)
			b = (b + 1) & 0xFF;
		cycles -= 2;
	}

	private final void inc_DE()
	{
		e = (e + 1) & 0xFF;
		if (e == 0x00)
			d = (d + 1) & 0xFF;
		cycles -= 2;
	}

	private final void inc_HL()
	{
		l = (l + 1) & 0xFF;
		if (l == 0x00)
			h = (h + 1) & 0xFF;
		cycles -= 2;
	}

	private final void inc_SP()
	{
		sp = (sp + 1) & 0xFFFF;
		cycles -= 2;
	}

	/*
	 * DEC rr
	 */
	private final void dec_BC()
	{
		c = (c - 1) & 0xFF;
		if (c == 0xFF)
			b = (b - 1) & 0xFF;
		cycles -= 2;
	}

	private final void dec_DE()
	{
		e = (e - 1) & 0xFF;
		if (e == 0xFF)
			d = (d - 1) & 0xFF;
		cycles -= 2;
	}

	private final void dec_HL()
	{
		l = (l - 1) & 0xFF;
		if (l == 0xFF)
			h = (h - 1) & 0xFF;
		cycles -= 2;
	}

	private final void dec_SP()
	{
		sp = (sp - 1) & 0xFFFF;
		cycles -= 2;
	}

	/*
	 * ADD SP,nn
	 */
	private final void add_SP_nn()
	{
		int offset = (byte) fetch();
		
		int s = (sp + offset) & 0xFFFF;
		
		if (offset >= 0)
			f = (s < sp ? C_FLAG : 0) + ((s & 0x0F00) < (sp & 0x0F00) ? H_FLAG : 0);
		else
			f = (s > sp ? C_FLAG : 0) + ((s & 0x0F00) > (sp & 0x0F00) ? H_FLAG : 0);
		
		sp = s;
		cycles -= 4;
	}

	/*
	 * LD HL,SP+nn
	 */
	private final void ld_HP_SP_nn()
	{
		byte offset = (byte) fetch();
		
		int s = (sp + offset) & 0xFFFF;
		
		if (offset >= 0)
			f = (s < sp ? C_FLAG : 0) + ((s & 0x0F00) < (sp & 0x0F00) ? H_FLAG : 0);
		else
			f = (s > sp ? C_FLAG : 0) + ((s & 0x0F00) > (sp & 0x0F00) ? H_FLAG : 0);
		
		l = s & 0xFF;
		h = s >> 8;
		
		cycles -= 3;
	}

	/*
	 * RLCA
	 */
	private final void rlca()
	{
		f = ((a & 0x80) != 0 ? C_FLAG : 0);
		a = ((a & 0x7F) << 1) + ((a & 0x80) >> 7);
		cycles -= 1;
	}

	/*
	 * RLA
	 */
	private final void rla()
	{
		int s = ((a & 0x7F) << 1) + ((f & C_FLAG) != 0 ? 0x01 : 0x00);
		f = ((a & 0x80) != 0 ? C_FLAG : 0);
		a = s;
		cycles -= 1;
	}

	/*
	 * RRCA
	 */
	private final void rrca()
	{
		f = ((a & 0x01) != 0 ? C_FLAG : 0);
		a = ((a >> 1) & 0x7F) + ((a << 7) & 0x80);
		cycles -= 1;
	}

	/*
	 * RRA
	 */
	private final void rra()
	{
		int s = ((a >> 1) & 0x7F) + ((f & C_FLAG) != 0 ? 0x80 : 0x00);
		f = ((a & 0x01) != 0 ? C_FLAG : 0);
		a = s;
		cycles -= 1;
	}

	/*
	 * RLC r
	 */
	private final void rlc_B()
	{
		b = rlc(b);
		cycles -= 2;
	}

	private final void rlc_C()
	{
		c = rlc(c);
		cycles -= 2;
	}

	private final void rlc_D()
	{
		d = rlc(d);
		cycles -= 2;
	}

	private final void rlc_E()
	{
		e = rlc(e);
		cycles -= 2;
	}

	private final void rlc_H()
	{
		h = rlc(h);
		cycles -= 2;
	}

	private final void rlc_L()
	{
		l = rlc(l);
		cycles -= 2;
	}

	private final void rlc_A()
	{
		a = rlc(a);
		cycles -= 2;
	}

	/*
	 * RLC (HL)
	 */
	private final void rlc_HLi()
	{
		write(h, l, rlc(read(h, l)));
		cycles -= 4;
	}

	/*
	 * RL r
	 */
	private final void rl_B()
	{
		b = rl(b);
		cycles -= 2;
	}

	private final void rl_C()
	{
		c = rl(c);
		cycles -= 2;
	}

	private final void rl_D()
	{
		d = rl(d);
		cycles -= 2;
	}

	private final void rl_E()
	{
		e = rl(e);
		cycles -= 2;
	}

	private final void rl_H()
	{
		h = rl(h);
		cycles -= 2;
	}

	private final void rl_L()
	{
		l = rl(l);
		cycles -= 2;
	}

	private final void rl_A()
	{
		a = rl(a);
		cycles -= 2;
	}

	/*
	 * RL (HL)
	 */
	private final void rl_HLi()
	{
		write(h, l, rl(read(h, l)));
		cycles -= 4;
	}

	/*
	 * RRC r
	 */
	private final void rrc_B()
	{
		b = rrc(b);
		cycles -= 2;
	}

	private final void rrc_C()
	{
		c = rrc(c);
		cycles -= 2;
	}

	private final void rrc_D()
	{
		d = rrc(d);
		cycles -= 2;
	}

	private final void rrc_E()
	{
		e = rrc(e);
		cycles -= 2;
	}

	private final void rrc_H()
	{
		h = rrc(h);
		cycles -= 2;
	}

	private final void rrc_L()
	{
		l = rrc(l);
		cycles -= 2;
	}

	private final void rrc_A()
	{
		a = rrc(a);
		cycles -= 2;
	}

	/*
	 * RRC (HL)
	 */
	private final void rrc_HLi()
	{
		write(h, l, rrc(read(h, l)));
		cycles -= 4;
	}

	/*
	 * RR r
	 */
	private final void rr_B()
	{
		b = rr(b);
		cycles -= 2;
	}

	private final void rr_C()
	{
		c = rr(c);
		cycles -= 2;
	}

	private final void rr_D()
	{
		d = rr(d);
		cycles -= 2;
	}

	private final void rr_E()
	{
		e = rr(e);
		cycles -= 2;
	}

	private final void rr_H()
	{
		h = rr(h);
		cycles -= 2;
	}

	private final void rr_L()
	{
		l = rr(l);
		cycles -= 2;
	}

	private final void rr_A()
	{
		a = rr(a);
		cycles -= 2;
	}

	/*
	 * RR (HL)
	 */
	private final void rr_HLi()
	{
		write(h, l, rr(read(h, l)));
		cycles -= 4;
	}

	/*
	 * SLA r
	 */
	private final void sla_B()
	{
		b = sla(b);
		cycles -= 2;
	}

	private final void sla_C()
	{
		c = sla(c);
		cycles -= 2;
	}

	private final void sla_D()
	{
		d = sla(d);
		cycles -= 2;
	}

	private final void sla_E()
	{
		e = sla(e);
		cycles -= 2;
	}

	private final void sla_H()
	{
		h = sla(h);
		cycles -= 2;
	}

	private final void sla_L()
	{
		l = sla(l);
		cycles -= 2;
	}

	private final void sla_A()
	{
		a = sla(a);
		cycles -= 2;
	}

	/*
	 * SLA (HL)
	 */
	private final void sla_HLi()
	{
		write(h, l, sla(read(h, l)));
		cycles -= 4;
	}

	/*
	 * SWAP r
	 */
	private final void swap_B()
	{
		b = swap(b);
		cycles -= 2;
	}

	private final void swap_C()
	{
		c = swap(c);
		cycles -= 2;
	}

	private final void swap_D()
	{
		d = swap(d);
		cycles -= 2;
	}

	private final void swap_E()
	{
		e = swap(e);
		cycles -= 2;
	}

	private final void swap_H()
	{
		h = swap(h);
		cycles -= 2;
	}

	private final void swap_L()
	{
		l = swap(l);
		cycles -= 2;
	}

	private final void swap_A()
	{
		a = swap(a);
		cycles -= 2;
	}

	/*
	 * SWAP (HL)
	 */
	private final void swap_HLi()
	{
		write(h, l, swap(read(h, l)));
		cycles -= 4;
	}

	/*
	 * SRA r
	 */
	private final void sra_B()
	{
		b = sra(b);
		cycles -= 2;
	}

	private final void sra_C()
	{
		c = sra(c);
		cycles -= 2;
	}

	private final void sra_D()
	{
		d = sra(d);
		cycles -= 2;
	}

	private final void sra_E()
	{
		e = sra(e);
		cycles -= 2;
	}

	private final void sra_H()
	{
		h = sra(h);
		cycles -= 2;
	}

	private final void sra_L()
	{
		l = sra(l);
		cycles -= 2;
	}

	private final void sra_A()
	{
		a = sra(a);
		cycles -= 2;
	}

	/*
	 * SRA (HL)
	 */
	private final void sra_HLi()
	{
		write(h, l, sra(read(h, l)));
		cycles -= 4;
	}

	/*
	 * SRL r
	 */
	private final void srl_B()
	{
		b = srl(b);
		cycles -= 2;
	}

	private final void srl_C()
	{
		c = srl(c);
		cycles -= 2;
	}

	private final void srl_D()
	{
		d = srl(d);
		cycles -= 2;
	}

	private final void srl_E()
	{
		e = srl(e);
		cycles -= 2;
	}

	private final void srl_H()
	{
		h = srl(h);
		cycles -= 2;
	}

	private final void srl_L()
	{
		l = srl(l);
		cycles -= 2;
	}

	private final void srl_A()
	{
		a = srl(a);
		cycles -= 2;
	}

	/*
	 * SRL (HL)
	 */
	private final void srl_HLi()
	{
		write(h, l, srl(read(h, l)));
		cycles -= 4;
	}

	/*
	 * BIT n,r
	 */
	private final void bit_B(int n)
	{
		bit(n, b);
		cycles -= 2;
	}

	private final void bit_C(int n)
	{
		bit(n, c);
		cycles -= 2;
	}

	private final void bit_D(int n)
	{
		bit(n, d);
		cycles -= 2;
	}

	private final void bit_E(int n)
	{
		bit(n, e);
		cycles -= 2;
	}

	private final void bit_H(int n)
	{
		bit(n, h);
		cycles -= 2;
	}

	private final void bit_L(int n)
	{
		bit(n, l);
		cycles -= 2;
	}

	private final void bit_A(int n)
	{
		bit(n, a);
		cycles -= 2;
	}

	/*
	 * BIT n,(HL)
	 */
	private final void bit_HLi(int n)
	{
		bit(n, read(h, l));
		cycles -= 3;
	}

	/*
	 * SET n,r
	 */
	private final void set_B(int n)
	{
		b |= 1 << n;
		cycles -= 2;
	}

	private final void set_C(int n)
	{
		c |= 1 << n;
		cycles -= 2;
	}

	private final void set_D(int n)
	{
		d |= 1 << n;
		cycles -= 2;
	}

	private final void set_E(int n)
	{
		e |= 1 << n;
		cycles -= 2;
	}

	private final void set_H(int n)
	{
		h |= 1 << n;
		cycles -= 2;
	}

	private final void set_L(int n)
	{
		l |= 1 << n;
		cycles -= 2;
	}

	private final void set_A(int n)
	{
		a |= 1 << n;
		cycles -= 2;
	}

	/*
	 * SET n,(HL)
	 */
	private final void set_HLi(int n)
	{
		write(h, l, read(h, l) | (1 << n));
		cycles -= 4;
	}

	/*
	 * RES n,r
	 */
	private final void res_B(int n)
	{
		b &= ~(1 << n);
		cycles -= 2;
	}

	private final void res_C(int n)
	{
		c &= ~(1 << n);
		cycles -= 2;
	}

	private final void res_D(int n)
	{
		d &= ~(1 << n);
		cycles -= 2;
	}

	private final void res_E(int n)
	{
		e &= ~(1 << n);
		cycles -= 2;
	}

	private final void res_H(int n)
	{
		h &= ~(1 << n);
		cycles -= 2;
	}

	private final void res_L(int n)
	{
		l &= ~(1 << n);
		cycles -= 2;
	}

	private final void res_A(int n)
	{
		a &= ~(1 << n);
		cycles -= 2;
	}

	/*
	 * RES n,(HL)
	 */
	private final void res_HLi(int n)
	{
		write(h, l, read(h, l) & ~(1 << n));
		cycles -= 4;
	}

	/*
	 * CCF/SCF
	 */
	private final void ccf()
	{
		f = (f & (Z_FLAG | C_FLAG)) ^ C_FLAG;
	}

	private final void scf()
	{
		f = (f & Z_FLAG) | C_FLAG;
	}

	/*
	 * NOP
	 */
	private final void nop()
	{
		cycles -= 1;
	}

	/*
	 * JP nnnn
	 */
	private final void jp_nnnn()
	{
		int lo = fetch();
		int hi = fetch();
		pc = (hi << 8) + lo;
		cycles -= 4;
	}

	/*
	 * LD PC,HL
	 */
	private final void ld_PC_HL()
	{
		pc = (h << 8) + l;
		cycles -= 1;
	}

	/*
	 * JP cc,nnnn
	 */
	private final void jp_cc_nnnn(boolean cc)
	{
		if (cc) {
			int lo = fetch();
			int hi = fetch();
			pc = (hi << 8) + lo;
			cycles -= 4;
		}
		else {
			pc = (pc + 2) & 0xFFFF;
			cycles -= 3;
		}
	}

	private final void jp_NZ_nnnn()
	{
		jp_cc_nnnn((f & Z_FLAG) == 0);
	}

	private final void jp_NC_nnnn()
	{
		jp_cc_nnnn((f & C_FLAG) == 0);
	}

	private final void jp_Z_nnnn()
	{
		jp_cc_nnnn((f & Z_FLAG) != 0);
	}

	private final void jp_C_nnnn()
	{
		jp_cc_nnnn((f & C_FLAG) != 0);
	}

	/*
	 * JR +nn
	 */
	private final void jr_nn()
	{
		byte offset = (byte) fetch();
		pc = (pc + offset) & 0xFFFF;
		cycles -= 3;
	}

	/*
	 * JR cc,+nn
	 */
	private final void jr_cc_nn(boolean cc)
	{
		if (cc) {
			byte offset = (byte) fetch();

			pc = (pc + offset) & 0xFFFF;
			cycles -= 3;
		}
		else {
			pc = (pc + 1) & 0xFFFF;
			cycles -= 2;
		}
	}

	private final void jr_NZ_nn()
	{
		jr_cc_nn((f & Z_FLAG) == 0);
	}

	private final void jr_Z_nn()
	{
		jr_cc_nn((f & Z_FLAG) != 0);
	}

	private final void jr_NC_nn()
	{
		jr_cc_nn((f & C_FLAG) == 0);
	}

	private final void jr_C_nn()
	{
		jr_cc_nn((f & C_FLAG) != 0);
	}

	/*
	 * CALL nnnn
	 */
	private final void call_nnnn()
	{
		int lo = fetch();
		int hi = fetch();
		call((hi << 8) + lo);
		cycles -= 6;
	}

	/*
	 * CALL cc,nnnn
	 */
	private final void call_cc_nnnn(boolean cc)
	{
		if (cc) {
			int lo = fetch();
			int hi = fetch();
			call((hi << 8) + lo);
			cycles -= 6;
		}
		else {
			pc = (pc + 2) & 0xFFFF;
			cycles -= 3;
		}
	}

	private final void call_NZ_nnnn()
	{
		call_cc_nnnn((f & Z_FLAG) == 0);
	}

	private final void call_NC_nnnn()
	{
		call_cc_nnnn((f & C_FLAG) == 0);
	}

	private final void call_Z_nnnn()
	{
		call_cc_nnnn((f & Z_FLAG) != 0);
	}

	private final void call_C_nnnn()
	{
		call_cc_nnnn((f & C_FLAG) != 0);
	}

	/*
	 * RET
	 */
	private final void ret()
	{
		int lo = pop();
		int hi = pop();
		pc = (hi << 8) + lo;
		cycles -= 4;
	}

	/*
	 * RET cc
	 */
	private final void ret_cc(boolean cc)
	{
		if (cc) {
			int lo = pop();
			int hi = pop();
			pc = (hi << 8) + lo;
			cycles -= 5;
		}
		else {
			cycles -= 2;
		}
	}

	private final void ret_NZ()
	{
		ret_cc((f & Z_FLAG) == 0);
	}

	private final void ret_NC()
	{
		ret_cc((f & C_FLAG) == 0);
	}

	private final void ret_Z()
	{
		ret_cc((f & Z_FLAG) != 0);
	}

	private final void ret_C()
	{
		ret_cc((f & C_FLAG) != 0);
	}

	/*
	 * RST nn
	 */
	private final void rst(int nn)
	{
		call(nn);
		cycles -= 4;
	}
	
	/*
	 * RETI
	 */
	private final void reti()
	{
		int lo = pop();
		int hi = pop();
		pc = (hi << 8) + lo;
		
		// enable interrupts
		ime = true;
		cycles -= 4;
	
		// execute next instruction
		execute();
	
		// check pending interrupts
		interrupt();
	}

	/*
	 * DI/EI
	 */
	private final void di()
	{
		// disable interrupts
		ime = false;
		cycles -= 1;
	}

	private final void ei()
	{
		// enable interrupts
		ime = true;
		cycles -= 1;
		
		// execute next instruction
		execute();

		// check pending interrupts
		interrupt();
	}	

	/*
	 * HALT/STOP
	 */
	private final void halt()
	{
		halted = true;

		// emulate bug when interrupts are pending
		if (!ime && interrupt.isPending())
			execute(memory.read(pc));
		
		// check pending interrupts
		interrupt();
	}

	private final void stop()
	{
		fetch();
	}
}
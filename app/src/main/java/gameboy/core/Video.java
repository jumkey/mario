/**
 * Mario GameBoy (TM) Emulator
 * 
 * LCD Video Display Processor
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

import gameboy.core.driver.VideoDriver;

public final class Video {
	/*
	 * LCD Register Addresses
	 */
	public static final int LCDC	= 0xFF40;		/* LCD Control */
	public static final int STAT	= 0xFF41;		/* LCD Status */
	public static final int SCY		= 0xFF42;		/* BG Scroll Y (0-255) */
	public static final int SCX		= 0xFF43;		/* BG Scroll X (0-255) */
	public static final int LY		= 0xFF44;		/* LCDC Y-Coordinate (0-153) */
	public static final int LYC		= 0xFF45;		/* LY Compare */
	public static final int DMA		= 0xFF46;		/* OAM DMA Transfer */
	public static final int BGP		= 0xFF47;		/* BG Palette Data */
	public static final int OBP0	= 0xFF48;		/* Object Palette 0 Data */
	public static final int OBP1	= 0xFF49;		/* Object Palette 1 Data */
	public static final int WY		= 0xFF4A;		/* Window Y Position (0-143) */
	public static final int WX		= 0xFF4B;		/* Window X Position (0-166) */
	
	/*
	 * OAM Register Addresses
	 */
	public static final int OAM_ADDR	= 0xFE00;	/* OAM Object Attribute Map (FE00..FE9F) */
	public static final int OAM_SIZE	= 0xA0;
	
	/*
	 * Video RAM Addresses
	 */
	public static final int VRAM_ADDR	= 0x8000;	/* 8KB Video RAM (8000..9FFF) */
	public static final int VRAM_SIZE	= 0x2000;

	/*
	 * VRAM Tile Data/Maps Addresses
	 */
	public static final int VRAM_DATA_A	= 0x0000;	/* 4KB Tile Data (8000..8FFF) */
	public static final int VRAM_DATA_B	= 0x0800;	/* 4KB Tile Data (8800..97FF) */

	public static final int VRAM_MAP_A	= 0x1800;	/* 1KB BG Tile Map 0 (9800..9BFF) */
	public static final int VRAM_MAP_B	= 0x1C00;	/* 1KB BG Tile Map 1 (9C00..9FFF) */
	
	/*
	 * Gameboy Clock Speed (1048576 Hz)
	 */
	public static final int GAMEBOY_CLOCK	= 1 << 20;
	
	/*
	 * LCD Mode Durations
	 */
	private static final int MODE_0_TICKS		= 50;	/* H-Blank */
	private static final int MODE_1_TICKS		= 114;	/* V-Blank */
	private static final int MODE_2_TICKS		= 20;	/* OAM     */
	private static final int MODE_3_BEGIN_TICKS	= 12;	/* Display */
	private static final int MODE_3_END_TICKS	= 32;	/* Display */
	
	private static final int MODE_1_BEGIN_TICKS	= 8;	/* V-Blank Line 144 */
	private static final int MODE_1_END_TICKS	= 1;	/* V-Blank Line 153 */
	
	/*
	 * Objects per Line
	 */
	private static final int OBJECTS_PER_LINE	= 10;
		
	/*
	 * LCD Color Palette
	 */
	private static final int COLOR_MAP[] = {
		0x9CB916, 0x8CAA14, 0x306430, 0x103F10
		// 0xE0F8D0, 0x88C070, 0x386850, 0x081820
		// 0xFFFFFF, 0xAAAAAA, 0x555555, 0x000000
	};

	/*
	 * OAM Registers
	 */
	private byte[] oam = new byte[OAM_SIZE];

	/*
	 * Video RAM
	 */
	private byte[] vram = new byte[VRAM_SIZE];
	
	/*
	 * LCD Registers 
	 */
	private int lcdc;
	private int stat;
	private int scy;
	private int scx;
	private int ly;
	private int lyc;
	private int dma;
	private int bgp;
	private int obp0;
	private int obp1;
	private int wy;
	private int wx;
	private int wly;
	
	private int cycles;
	
	private int frames;
	private int frameSkip;
	
	private boolean transfer;
	private boolean display;
	private boolean vblank;
	private boolean dirty;

	/*
	 * Line Buffer, OAM Cache and Color Palette
	 */
	private int[] line = new int[8 + 160 + 8];
	private int[] objects = new int[OBJECTS_PER_LINE];
	private int[] palette = new int[1024];

	/*
	 * Video Driver
	 */
	private VideoDriver driver;
		
	/*
	 * Interrupt Controller
	 */
	private Interrupt interrupt;
	
	/*
	 * Memory Interface
	 */
	private Memory memory;
	

	public Video(VideoDriver driver, Interrupt interrupt, Memory memory)
	{
		this.driver = driver;
		this.interrupt = interrupt;
		this.memory = memory;
		
		reset();
	}

	public final int getFrameSkip()
	{
		return frameSkip;
	}
	
	public final void setFrameSkip(int frameSkip)
	{
		this.frameSkip = frameSkip;
	}
	
	public final void reset()
	{
		cycles = MODE_2_TICKS;
		
		lcdc = 0x91; 
		stat = 2;
		ly = 0;
		lyc = 0;
		dma = 0xFF;
		scy = 0;
		scx = 0;
		wy = wly = 0;
		wx = 0;
		bgp = 0xFC;
		obp0 = obp1 = 0xFF;
		
		transfer = true;
		display = true;
		vblank = true;
		dirty = true;
		
		for (int index = 0; index < vram.length; index++)
			vram[index] = 0x00;
		
		for (int index = 0; index < oam.length; index++)
			oam[index] = 0x00;
	}

	public final void write(int address, int data)
	{
		// assert data >= 0x00 && data <= 0xFF;
		
		switch (address) {
		case LCDC:
			setControl(data);
			break;
			
		case STAT:
			setStatus(data);
			break;
			
		case SCY:
			setScrollY(data);
			break;
			
		case SCX:
			setScrollX(data);
			break;
			
		case LY:
			// Read Only
			break;
			
		case LYC:
			setLYCompare(data);
			break;
			
		case DMA:
			setDMA(data);
			break;
			
		case BGP:
			setBackgroundPalette(data);
			break;
			
		case OBP0:
			setObjectPalette0(data);
			break;
			
		case OBP1:
			setObjectPalette1(data);
			break;

		case WY:
			setWindowY(data);
			break;
			
		case WX:
			setWindowX(data);
			break;
			
		default:
			if (address >= OAM_ADDR	&& address < OAM_ADDR + OAM_SIZE) {
				oam[address - OAM_ADDR] = (byte) data;
			}
			else if (address >= VRAM_ADDR && address < VRAM_ADDR + VRAM_SIZE) {
				vram[address - VRAM_ADDR] = (byte) data;
			}
			break;
		}
	}

	public final int read(int address)
	{
		switch (address) {
		case LCDC:
			return getControl();
			
		case STAT:
			return getStatus();
			
		case SCY:
			return getScrollY();
			
		case SCX:
			return getScrollX();
			
		case LY:
			return getLineY();
			
		case LYC:
			return getLineYCompare();
			
		case DMA:
			return getDMA();
			
		case BGP:
			return getBackgroundPalette();
			
		case OBP0:
			return getObjectPalette0();
			
		case OBP1:
			return getObjectPalette1();

		case WY:
			return getWindowY();
			
		case WX:
			return getWindowX();
			
		default:
			if (address >= OAM_ADDR	&& address < OAM_ADDR + OAM_SIZE) {
				return oam[address - OAM_ADDR] & 0xFF;
			}
			else if (address >= VRAM_ADDR && address < VRAM_ADDR + VRAM_SIZE) {
				return vram[address - VRAM_ADDR] & 0xFF;
			}
		}
		
		return 0xFF;
	}
	
	public final int cycles()
	{
		return cycles;
	}
	
	public final void emulate(int ticks)
	{
		if ((lcdc & 0x80) != 0) {
			cycles -= ticks;

			while (cycles <= 0) {
				switch (stat & 0x03) {
				case 0:
					emulateHBlank();
					break;
				case 1:
					emulateVBlank();
					break;
				case 2:
					emulateOAM();
					break;
				case 3:
					emulateTransfer();
					break;
				}
			}
		}
	}

	private final int getControl()
	{
		return lcdc;
	}

	private final int getStatus()
	{
		return 0x80 | stat;
	}

	private final int getScrollY()
	{
		return scy;
	}

	private final int getScrollX()
	{
		return scx;
	}

	private final int getLineY()
	{
		return ly;
	}

	private final int getLineYCompare()
	{
		return lyc;
	}

	private final int getDMA()
	{
		return dma;
	}

	private final int getBackgroundPalette()
	{
		return bgp;
	}

	private final int getObjectPalette0()
	{
		return obp0;
	}

	private final int getObjectPalette1()
	{
		return obp1;
	}

	private final int getWindowY()
	{
		return wy;
	}

	private final int getWindowX()
	{
		return wx;
	}
	
	private final void setControl(int data)
	{
		if ((lcdc & 0x80) != (data & 0x80)) {			
			// NOTE: do not reset LY=LYC flag (bit 2) of the STAT register (Mr. Do!)
			if ((data & 0x80) != 0) {
				stat = (stat & 0xFC) | 0x02;
				cycles = MODE_2_TICKS;
				ly = 0;
				
				display = false;
			}
			else {
				stat = (stat & 0xFC) | 0x00;
				cycles = MODE_1_TICKS;
				ly = 0;
				
				clearFrame();
			}
		}

		// don't draw window if it was not enabled and not being drawn before
		if ((lcdc & 0x20) == 0 && (data & 0x20) != 0 && wly == 0 && ly > wy)
			wly = 144;

		lcdc = data;
	}

	private final void setStatus(int data)
	{
		stat = (stat & 0x87) | (data & 0x78);

		// Gameboy Bug
		if ((lcdc & 0x80) != 0 && (stat & 0x03) == 0x01 && (stat & 0x44) != 0x44)
			interrupt.raise(Interrupt.LCD);
	}

	private final void setScrollY(int data)
	{
		scy = data;
	}

	private final void setScrollX(int data)
	{
		scx = data;
	}

	private final void setLYCompare(int data)
	{
		lyc = data;
		
		if ((lcdc & 0x80) != 0) {
			if (ly == lyc) {
				// NOTE: raise interrupt once per line (Prehistorik Man, The Jetsons, Muhammad Ali)
				if ((stat & 0x04) == 0) {
					// LYC=LY interrupt
					stat |= 0x04;
	
					if ((stat & 0x40) != 0)
						interrupt.raise(Interrupt.LCD);
				}
			}					
			else {
				stat &= 0xFB;
			}
		}
	}

	private final void setDMA(int data)
	{
		dma = data;
		
		for (int index = 0; index < OAM_SIZE; index++)
			oam[index] = (byte) memory.read((dma << 8) + index);
	}
	
	private final void setBackgroundPalette(int data)
	{
		if (bgp != data) {
			bgp = data;
		
			dirty = true;
		}
	}
	
	private final void setObjectPalette0(int data)
	{
		if (obp0 != data) {
			obp0 = data;
		
			dirty = true;
		}
	}

	private final void setObjectPalette1(int data)
	{
		if (obp1 != data) {
			obp1 = data;
		
			dirty = true;
		}
	}

	private final void setWindowY(int data)
	{
		wy = data;
	}

	private final void setWindowX(int data)
	{
		wx = data;
	}
	
	private final void emulateOAM()
	{
		stat = (stat & 0xFC) | 0x03;
		cycles += MODE_3_BEGIN_TICKS;
		
		transfer = true;
	}

	private final void emulateTransfer()
	{
		if (transfer) {
			if (display)
				drawLine();
			
			stat = (stat & 0xFC) | 0x03;
			cycles += MODE_3_END_TICKS;
			transfer = false;
		}
		else {
			stat = (stat & 0xFC) | 0x00;
			cycles += MODE_0_TICKS;
			
			// H-Blank interrupt
			if ((stat & 0x08) != 0 && (stat & 0x44) != 0x44)
				interrupt.raise(Interrupt.LCD);
		}
	}

	private final void emulateHBlank()
	{
		ly++;
		
		if (ly == lyc) {
			// LYC=LY interrupt
			stat |= 0x04;
			if ((stat & 0x40) != 0)
				interrupt.raise(Interrupt.LCD);
		}					
		else {
			stat &= 0xFB;
		}
		
		if (ly < 144) {
			stat = (stat & 0xFC) | 0x02;
			cycles += MODE_2_TICKS;
			
			// OAM interrupt
			if ((stat & 0x20) != 0 && (stat & 0x44) != 0x44)
				interrupt.raise(Interrupt.LCD);
		}
		else {
			if (display)
				drawFrame();
			
			if (frames++ >= frameSkip) {
				display = true;
				frames = 0;
			}
			else {
				display = false;
			}

			stat = (stat & 0xFC) | 0x01;
			cycles += MODE_1_BEGIN_TICKS;

			vblank = true;
		}
	}

	private final void emulateVBlank()
	{
		if (vblank) {
			vblank = false;
			
			stat = (stat & 0xFC) | 0x01;
			cycles += MODE_1_TICKS - MODE_1_BEGIN_TICKS;

			// V-Blank interrupt
			if ((stat & 0x10) != 0)
				interrupt.raise(Interrupt.LCD);
			
			// V-Blank interrupt
			interrupt.raise(Interrupt.VBLANK);
		}
		else if (ly == 0) {
			stat = (stat & 0xFC) | 0x02;
			cycles += MODE_2_TICKS;

			// OAM interrupt
			if ((stat & 0x20) != 0 && (stat & 0x44) != 0x44) 
				interrupt.raise(Interrupt.LCD);
		}
		else {
			if (ly < 153) {
				ly++;

				stat = (stat & 0xFC) | 0x01;

				if (ly == 153)
					cycles += MODE_1_END_TICKS;
				else
					cycles += MODE_1_TICKS;
			}
			else {
				ly = wly = 0;

				stat = (stat & 0xFC) | 0x01;
				cycles += MODE_1_TICKS - MODE_1_END_TICKS;
			}
			
			if (ly == lyc) {
				// LYC=LY interrupt
				stat |= 0x04;

				if ((stat & 0x40) != 0)
					interrupt.raise(Interrupt.LCD);
			}
			else {
				stat &= 0xFB;
			}
		}
	}

	private final void drawFrame()
	{
		driver.display();
	}

	private final void clearFrame()
	{
		clearPixels();
		
		driver.display();
	}
	
	private final void drawLine()
	{
		if ((lcdc & 0x01) != 0)
			drawBackground();
		else
			drawCleanBackground();
		
		if ((lcdc & 0x20) != 0)
			drawWindow();
		
		if ((lcdc & 0x02) != 0)
			drawObjects();
		
		drawPixels();
	}
	
	private final void drawCleanBackground()
	{
		for (int x = 0; x < 8+160+8; x++)
			line[x] = 0x00;
	}
	
	private final void drawBackground()
	{
		final int y = (scy + ly) & 0xFF;
		final int x = scx & 0xFF;
		
		int tileMap = (lcdc & 0x08) != 0 ? VRAM_MAP_B : VRAM_MAP_A;
		int tileData = (lcdc & 0x10) != 0 ? VRAM_DATA_A : VRAM_DATA_B;

		tileMap += ((y >> 3) << 5) + (x >> 3);			
		tileData += (y & 7) << 1; 
		
		drawTiles(8 - (x & 7), tileMap, tileData);
	}

	private final void drawWindow()
	{
		if (ly >= wy && wx < 167 && wly < 144) {
			int tileMap = (lcdc & 0x40) != 0 ? VRAM_MAP_B : VRAM_MAP_A;
			int tileData = (lcdc & 0x10) != 0 ? VRAM_DATA_A : VRAM_DATA_B;

			tileMap += (wly >> 3) << 5;
			tileData += (wly & 7) << 1;
			
			drawTiles(wx + 1, tileMap, tileData);
			
			wly++;
		}
	}

	private final void drawObjects()
	{
		int count = scanObjects();
		
		for (int lastx = 176, index = 0; index < count; index++) {
			int data = objects[index];
			
			int x = (data >> 24) & 0xFF;
			int flags = (data >> 12) & 0xFF;
			int address = data & 0xFFF;

			if (x + 8 <= lastx)
				drawObjectTile(x, address, flags);
			else
				drawOverlappedObjectTile(x, address, flags);
			
			lastx = x;
		}
	}

	private final int scanObjects()
	{
		int count = 0;

		// search active objects
		for (int offset = 0; offset < 4*40; offset += 4) {
			int y = oam[offset + 0] & 0xFF;
			int x = oam[offset + 1] & 0xFF;
			
			if (y <= 0 || y >= 144+16 || x <= 0 || x >= 168)
				continue;
			
			int tile = oam[offset + 2] & 0xFF;
			int flags = oam[offset + 3] & 0xFF;

			y = ly - y + 16;
			
			if ((lcdc & 0x04) != 0) {
				// 8x16 tile size
				if (y < 0 || y > 15)
					continue;
		
				// Y flip
				if ((flags & 0x40) != 0)
					y = 15 - y;
				
				tile &= 0xFE;
			}
			else {
				// 8x8 tile size
				if (y < 0 || y > 7)
					continue;
				
				// Y flip
				if ((flags & 0x40) != 0)
					y = 7 - y;
			}
			
			objects[count] = (x << 24) + (count << 20) + (flags << 12) + ((tile << 4) + (y << 1));
			
			if (++count >= OBJECTS_PER_LINE)
				break;
		}
		
		// sort objects from lower to higher priority
		for (int index = 0; index < count; index++) {
			int rightmost = index;
			
			for (int number = index + 1; number < count; number++) {
				if ((objects[number] >> 20) > (objects[rightmost] >> 20))
					rightmost = number;
			}
			
			if (rightmost != index) {
				int data = objects[index];
				objects[index] = objects[rightmost];
				objects[rightmost] = data;
			}
		}

		return count;
	}
	
	private final void drawTiles(int x, int tileMap, int tileData)
	{
		if ((lcdc & 0x10) != 0) {
			while (x < 168) {
				int tile = vram[tileMap] & 0xFF;
				
				drawTile(x, tileData + (tile << 4));

				tileMap = (tileMap & 0x1FE0) + ((tileMap + 1) & 0x001F);
				
				x += 8;
			}
		}
		else {
			while (x < 168) {
				int tile = (vram[tileMap] ^ 0x80) & 0xFF;
				
				drawTile(x, tileData + (tile << 4));

				tileMap = (tileMap & 0x1FE0) + ((tileMap + 1) & 0x001F);
				
				x += 8;
			}
		}
	}

	private final void drawTile(int x, int address)
	{
		int pattern = (vram[address] & 0xFF) + ((vram[address + 1] & 0xFF) << 8);
		
		line[x + 0] = (pattern >> 7) & 0x0101;
		line[x + 1] = (pattern >> 6) & 0x0101;
		line[x + 2] = (pattern >> 5) & 0x0101;
		line[x + 3] = (pattern >> 4) & 0x0101;
		line[x + 4] = (pattern >> 3) & 0x0101;
		line[x + 5] = (pattern >> 2) & 0x0101;
		line[x + 6] = (pattern >> 1) & 0x0101;
		line[x + 7] = (pattern >> 0) & 0x0101;
	}
	
	private final void drawObjectTile(int x, int address, int flags)
	{
		int pattern = (vram[address] & 0xFF) + ((vram[address + 1] & 0xFF) << 8);
		
		int mask = 0;
		
		// priority
		if ((flags & 0x80) != 0)
			mask |= 0x0008;

		// palette
		if ((flags & 0x10) != 0)
			mask |= 0x0004;
		
		// X flip
		if ((flags & 0x20) != 0) {
			int color;
			
			if ((color = (pattern << 1) & 0x0202) != 0)
				line[x + 0] |= color | mask;
			if ((color = (pattern >> 0) & 0x0202) != 0)
				line[x + 1] |= color | mask;
			if ((color = (pattern >> 1) & 0x0202) != 0)
				line[x + 2] |= color | mask;
			if ((color = (pattern >> 2) & 0x0202) != 0)
				line[x + 3] |= color | mask;
			if ((color = (pattern >> 3) & 0x0202) != 0)
				line[x + 4] |= color | mask;
			if ((color = (pattern >> 4) & 0x0202) != 0)
				line[x + 5] |= color | mask;
			if ((color = (pattern >> 5) & 0x0202) != 0)
				line[x + 6] |= color | mask;
			if ((color = (pattern >> 6) & 0x0202) != 0)
				line[x + 7] |= color | mask;
		}
		else {
			int color;

			if ((color = (pattern >> 6) & 0x0202) != 0)
				line[x + 0] |= color | mask;
			if ((color = (pattern >> 5) & 0x0202) != 0)
				line[x + 1] |= color | mask;
			if ((color = (pattern >> 4) & 0x0202) != 0)
				line[x + 2] |= color | mask;
			if ((color = (pattern >> 3) & 0x0202) != 0)
				line[x + 3] |= color | mask;
			if ((color = (pattern >> 2) & 0x0202) != 0)
				line[x + 4] |= color | mask;
			if ((color = (pattern >> 1) & 0x0202) != 0)
				line[x + 5] |= color | mask;
			if ((color = (pattern >> 0) & 0x0202) != 0)
				line[x + 6] |= color | mask;
			if ((color = (pattern << 1) & 0x0202) != 0)
				line[x + 7] |= color | mask;
		}
	}

	private final void drawOverlappedObjectTile(int x, int address, int flags)
	{
		int pattern = (vram[address] & 0xFF) + ((vram[address + 1] & 0xFF) << 8);
		
		int mask = 0;
		
		// priority
		if ((flags & 0x80) != 0)
			mask |= 0x0008;

		// palette
		if ((flags & 0x10) != 0)
			mask |= 0x0004;
		
		// X flip
		if ((flags & 0x20) != 0) {
			int color;
			
			if ((color = (pattern << 1) & 0x0202) != 0)
				line[x + 0] = (line[x + 0] & 0x0101) | color | mask;
			if ((color = (pattern >> 0) & 0x0202) != 0)
				line[x + 1] = (line[x + 1] & 0x0101) | color | mask;
			if ((color = (pattern >> 1) & 0x0202) != 0)
				line[x + 2] = (line[x + 2] & 0x0101) | color | mask;
			if ((color = (pattern >> 2) & 0x0202) != 0)
				line[x + 3] = (line[x + 3] & 0x0101) | color | mask;
			if ((color = (pattern >> 3) & 0x0202) != 0)
				line[x + 4] = (line[x + 4] & 0x0101) | color | mask;
			if ((color = (pattern >> 4) & 0x0202) != 0)
				line[x + 5] = (line[x + 5] & 0x0101) | color | mask;
			if ((color = (pattern >> 6) & 0x0202) != 0)
				line[x + 7] = (line[x + 7] & 0x0101) | color | mask;
			if ((color = (pattern >> 5) & 0x0202) != 0)
				line[x + 6] = (line[x + 6] & 0x0101) | color | mask;
		}
		else {
			int color;
			
			if ((color = (pattern >> 6) & 0x0202) != 0)
				line[x + 0] = (line[x + 0] & 0x0101) | color | mask;
			if ((color = (pattern >> 5) & 0x0202) != 0)
				line[x + 1] = (line[x + 1] & 0x0101) | color | mask;
			if ((color = (pattern >> 4) & 0x0202) != 0)
				line[x + 2] = (line[x + 2] & 0x0101) | color | mask;
			if ((color = (pattern >> 3) & 0x0202) != 0)
				line[x + 3] = (line[x + 3] & 0x0101) | color | mask;
			if ((color = (pattern >> 2) & 0x0202) != 0)
				line[x + 4] = (line[x + 4] & 0x0101) | color | mask;
			if ((color = (pattern >> 1) & 0x0202) != 0)
				line[x + 5] = (line[x + 5] & 0x0101) | color | mask;
			if ((color = (pattern >> 0) & 0x0202) != 0)
				line[x + 6] = (line[x + 6] & 0x0101) | color | mask;
			if ((color = (pattern << 1) & 0x0202) != 0)
				line[x + 7] = (line[x + 7] & 0x0101) | color | mask;
		}
	}
	
	private final void drawPixels()
	{
		updatePalette();

		int[] pixels = driver.getPixels();
		
		int offset = ly * driver.getWidth();
		
		for (int x = 8; x < 168; x += 4) {
			int pattern0 = line[x + 0];
			int pattern1 = line[x + 1];
			int pattern2 = line[x + 2];
			int pattern3 = line[x + 3];
			
			pixels[offset + 0] = palette[pattern0];
			pixels[offset + 1] = palette[pattern1];
			pixels[offset + 2] = palette[pattern2];
			pixels[offset + 3] = palette[pattern3];
			
			offset += 4;
		}
	}

	private final void clearPixels()
	{
		int[] pixels = driver.getPixels();
		
		int length = driver.getWidth() * driver.getHeight();
		
		for (int offset = 0; offset < length; offset++)
			pixels[offset] = COLOR_MAP[0];
	}
	
	private final void updatePalette()
	{
		if (dirty) {
			// bit 4/0 = BG color, bit 5/1 = OBJ color, bit 2 = OBJ palette, bit 3 = OBJ priority
			for (int pattern = 0; pattern < 64; pattern++) {
				int color;
				
				if ((pattern & 0x22) == 0 || ((pattern & 0x08) != 0 && (pattern & 0x11) != 0)) {
					// OBJ behind BG color 1-3
					color = (bgp >> ((((pattern >> 3) & 0x02) + (pattern & 0x01)) << 1)) & 0x03;
				}
				else {
					// OBJ above BG
					if ((pattern & 0x04) == 0)
						color = (obp0 >> ((((pattern >> 4) & 0x02) + ((pattern >> 1) & 0x01)) << 1)) & 0x03;
					else
						color = (obp1 >> ((((pattern >> 4) & 0x02) + ((pattern >> 1) & 0x01)) << 1)) & 0x03;
				}

				palette[((pattern & 0x30) << 4) + (pattern & 0x0F)] = COLOR_MAP[color];
			}
			
			dirty = false;
		}
	}
	
	public final String toString()
	{
		return "LCDC=" + Integer.toHexString(lcdc) + 
			   " STAT=" + Integer.toHexString(stat) +
			   " SCY=" + Integer.toHexString(scy) +
			   " SCX=" + Integer.toHexString(scx) +
			   " LY=" + Integer.toHexString(ly) +
			   " LYC=" + Integer.toHexString(lyc) +
			   " DMA=" + Integer.toHexString(dma) +
			   " BGP=" + Integer.toHexString(bgp) +
			   " OBP=" + Integer.toHexString(obp0) + "/" + Integer.toHexString(obp1) +
			   " WY=" + Integer.toHexString(wy) +
			   " WX=" + Integer.toHexString(wx) +
			   " WLY=" + Integer.toHexString(wly) + 
			   " cycles=" + cycles;
	}
}

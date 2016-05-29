/**
 * Mario GameBoy (TM) Emulator
 * 
 * Audio Processor Unit (Sharp LR35902 APU)
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

import gameboy.core.driver.SoundDriver;

public final class Sound {
	/*
	 * Gameboy Clock Speed (1048576 Hz)
	 */
	public static final int GAMEBOY_CLOCK		= 1 << 20;
	
	/*
	 * Sound Clock (256 Hz)
	 */
	public static final int SOUND_CLOCK			= 256;
	
	/*
	 * Sound Register Addresses
	 */
	public static final int NR10 = 0xFF10;		/* AUD1SWEEP */
	public static final int NR11 = 0xFF11;		/* AUD1LEN */
	public static final int NR12 = 0xFF12;		/* AUD1ENV */
	public static final int NR13 = 0xFF13;		/* AUD1LOW */
	public static final int NR14 = 0xFF14;		/* AUD1HIGH */
	
	public static final int NR21 = 0xFF16;		/* AUD2LEN */
	public static final int NR22 = 0xFF17;		/* AUD2ENV */
	public static final int NR23 = 0xFF18;		/* AUD2LOW */
	public static final int NR24 = 0xFF19;		/* AUD2HIGH */
	
	public static final int NR30 = 0xFF1A;		/* AUD3ENA */
	public static final int NR31 = 0xFF1B;		/* AUD3LEN */
	public static final int NR32 = 0xFF1C;		/* AUD3LEVEL */
	public static final int NR33 = 0xFF1D;		/* AUD3LOW */
	public static final int NR34 = 0xFF1E;		/* AUD3HIGH */
	
	public static final int NR41 = 0xFF20;		/* AUD4LEN */
	public static final int NR42 = 0xFF21;		/* AUD4ENV */
	public static final int NR43 = 0xFF22;		/* AUD4POLY */
	public static final int NR44 = 0xFF23;		/* AUD4GO */
	
	public static final int NR50 = 0xFF24;		/* AUDVOL */
	public static final int NR51 = 0xFF25;		/* AUDTERM */
	public static final int NR52 = 0xFF26;		/* AUDENA */

	public static final int AUD3WAVERAM = 0xFF30;

	/*
	 * Audio Channel 1
	 */
	private int nr10;
	private int nr11;
	private int nr12;
	private int nr13;
	private int nr14;
	
    private int audio1Index;
    private int audio1Length;
    private int audio1Volume;
    private int audio1EnvelopeLength;
    private int audio1SweepLength;
    private int audio1Frequency;

    /*
     * Audio Channel 2
     */
    private int nr21;
    private int nr22;
    private int nr23;
    private int nr24;
    
    private int audio2Index;
    private int audio2Length;
    private int audio2Volume;
    private int audio2EnvelopeLength;
    private int audio2Frequency;
    
    /*
     * Audio Channel 3
     */
    private int nr30;
    private int nr31;
    private int nr32;
    private int nr33;
    private int nr34;
    
    private int audio3Index;
    private int audio3Length;
    private int audio3Frequency;
    private byte[] audio3WavePattern = new byte[16];
    
    /*
     * Audio Channel 4
     */
    private int nr41;
    private int nr42;
    private int nr43;
    private int nr44;
    
    private int audio4Index;
    private int audio4Length;
    private int audio4Volume;
    private int audio4EnvelopeLength;
    private int audio4Frequency;
    
    /*
     * Output Control
     */
    private int nr50;
    private int nr51;
    private int nr52;

    /*
     * Sound Driver
     */
    private SoundDriver driver;
	private byte[] buffer = new byte[512];
	
	private int frames;
	private int cycles;
	
    /*
     * Frequency Table
     */
    private int[] frequencyTable = new int[2048];
	private int[] noiseFreqRatioTable = new int[8];

	/*
	 * Noise Tables
	 */
	private int[] noiseStep7Table = new int[128 / 32];
	private int[] noiseStep15Table = new int[32768 / 32];
	

	public Sound(SoundDriver soundDriver)
	{
		driver = soundDriver;
		
		generateFrequencyTables();
		
		generateNoiseTables();
		
		reset();
	}

	public final void start()
	{
		driver.start();
	}

	public final void stop()
	{
		driver.stop();
	}
	
	public final int cycles()
	{
		return cycles;
	}
	
	public final void emulate(int ticks)
	{
		cycles -= ticks;
		
		while (cycles <= 0) {			
			updateAudio();
			
			if (driver.isEnabled()) {
				frames += driver.getSampleRate();
				
				int length = (frames / SOUND_CLOCK) << 1;
				
				mixAudio(buffer, length);
				
				driver.write(buffer, length);
			
				frames %= SOUND_CLOCK;
			}
			
			cycles += GAMEBOY_CLOCK / SOUND_CLOCK;
		}
	}
	
	public final void reset()
	{
		cycles = GAMEBOY_CLOCK / SOUND_CLOCK;
		frames = 0;

		audio1Index = audio2Index = audio3Index = audio4Index = 0;
		
		write(NR10, 0x80);
		write(NR11, 0x3F); // 0xBF
		write(NR12, 0x00); // 0xF3
		write(NR13, 0xFF);
		write(NR14, 0xBF);
		
		write(NR21, 0x3F);
		write(NR22, 0x00);
		write(NR23, 0xFF);
		write(NR24, 0xBF);
		
		write(NR30, 0x7F);
		write(NR31, 0xFF);
		write(NR32, 0x9F);
		write(NR33, 0xFF);
		write(NR34, 0xBF);
		
		write(NR41, 0xFF);
		write(NR42, 0x00);
		write(NR43, 0x00);
		write(NR44, 0xBF);
		
		write(NR50, 0x00); // 0x77
		write(NR51, 0xF0);
		write(NR52, 0xFF); // 0xF0

		for (int address = 0xFF30; address <= 0xFF3F; address++)
			write(address, (address & 1) == 0 ? 0x00 : 0xFF);
	}

	public final int read(int address)
	{
		switch (address) {
		case NR10:
			return getAudio1Sweep();
		case NR11:
			return getAudio1Length();
		case NR12:
			return getAudio1Envelope();
		case NR13:
			return getAudio1Frequency();
		case NR14:
			return getAudio1Playback();

		case NR21:
			return getAudio2Length();
		case NR22:
			return getAudio2Envelope();
		case NR23:
			return getAudio2Frequency();
		case NR24:
			return getAudio2Playback();

		case NR30:
			return getAudio3Enable();
		case NR31:
			return getAudio3Length();
		case NR32:
			return getAudio3Level();
		case NR33:
			return getAudio4Frequency();
		case NR34:
			return getAudio3Playback();

		case NR41:
			return getAudio4Length();
		case NR42:
			return getAudio4Envelope();
		case NR43:
			return getAudio4Polynomial();
		case NR44:
			return getAudio4Playback();

		case NR50:
			return getOutputLevel();
		case NR51:
			return getOutputTerminal();
		case NR52:
			return getOutputEnable();
		
		default:
			if (address >= AUD3WAVERAM && address <= AUD3WAVERAM + 0x3F)
				return getAudio3WavePattern(address);
			break;
		}
		
		return 0xFF;
	}

	public final void write(int address, int data)
	{
		switch (address) {
		case NR10:
			setAudio1Sweep(data);
		    break;
		case NR11:
		    setAudio1Length(data);
		    break;
		case NR12:
			setAudio1Envelope(data);
			break;
		case NR13:
			setAudio1Frequency(data);
			break;
		case NR14:
			setAudio1Playback(data);
			break;
			
		case NR21:
			setAudio2Length(data);
			break;
		case NR22:
			setAudio2Envelope(data);
			break;
		case NR23:
			setAudio2Frequency(data);
			break;
		case NR24:
			setAudio2Playback(data);
			break;
			
		case NR30:
			setAudio3Enable(data);
			break;
		case NR31:
			setAudio3Length(data);
			break;
		case NR32:
			setAudio3Level(data);
			break;
		case NR33:
			setAudio3Frequency(data);
			break;
		case NR34:
			setAudio3Playback(data);
			break;
			
		case NR41:
			setAudio4Length(data);
			break;
		case NR42:
			setAudio4Envelope(data);
			break;
		case NR43:
			setAudio4Polynomial(data);
			break;
		case NR44:
			setAudio4Playback(data);
			break;
			
		case NR50:
			setOutputLevel(data);
			break;			
		case NR51:
			setOutputTerminal(data);
			break;
		case NR52:
			setOutputEnable(data);
			break;

		default:
			if (address >= AUD3WAVERAM && address <= AUD3WAVERAM + 0x3F)
				setAudio3WavePattern(address, data);
			break;
		}
	}
	
	private final void updateAudio()
	{
		if ((nr52 & 0x80) != 0) {
			if ((nr52 & 0x01) != 0)
				updateAudio1();
			
			if ((nr52 & 0x02) != 0)
				updateAudio2();
			
			if ((nr52 & 0x04) != 0)
				updateAudio3();
			
			if ((nr52 & 0x08) != 0)
				updateAudio4();				
		}
	}
	
	private final void mixAudio(byte[] buffer, int length)
	{
		for (int index = 0; index < length; index++) {
			buffer[index] = 0;
		}

		if ((nr52 & 0x80) != 0) {
			if ((nr52 & 0x01) != 0)
				mixAudio1(buffer, length);
			
			if ((nr52 & 0x02) != 0)
				mixAudio2(buffer, length);
			
			if ((nr52 & 0x04) != 0)
				mixAudio3(buffer, length);
			
			if ((nr52 & 0x08) != 0)
				mixAudio4(buffer, length);
		}
	}	
	
	/*
	 * Audio Channel 1
	 */
	private final int getAudio1Sweep()
	{
		return nr10;
	}

	private final int getAudio1Length()
	{
		return nr11;
	}

	private final int getAudio1Envelope()
	{
		return nr12;
	}

	private final int getAudio1Frequency()
	{
		return nr13;
	}

	private final int getAudio1Playback()
	{
		return nr14;
	}

	private final void setAudio1Sweep(int data)
	{
		nr10 = data;
		
		audio1SweepLength = (SOUND_CLOCK / 128) * ((nr10 >> 4) & 0x07);
	}
	
	private final void setAudio1Length(int data)
	{
		nr11 = data;

		audio1Length = (SOUND_CLOCK / 256) * (64 - (nr11 & 0x3F));
	}

	private final void setAudio1Envelope(int data)
	{
		nr12 = data;
		
		if ((nr14 & 0x40) == 0) {
			if ((nr12 >> 4) == 0) {
				audio1Volume = 0;
			}
			else {
				if (audio1EnvelopeLength == 0 && (nr12 & 0x07) == 0)
					audio1Volume = (audio1Volume + 1) & 0x0F;
				else
					audio1Volume = (audio1Volume + 2) & 0x0F;
			}
		}
	}

	private final void setAudio1Frequency(int data)
	{
		nr13 = data;
		
		audio1Frequency = frequencyTable[nr13 + ((nr14 & 0x07) << 8)];
	}

	private final void setAudio1Playback(int data)
	{
		nr14 = data;
		
		audio1Frequency = frequencyTable[nr13 + ((nr14 & 0x07) << 8)];
		
		if ((nr14 & 0x80) != 0) {
			nr52 |= 0x01;

			if ((nr14 & 0x40) != 0 && audio1Length == 0)
				audio1Length = (SOUND_CLOCK / 256) * (64 - (nr11 & 0x3F));

			audio1SweepLength = (SOUND_CLOCK / 128) * ((nr10 >> 4) & 0x07);
			
			audio1Volume = nr12 >> 4;
			audio1EnvelopeLength = (SOUND_CLOCK / 64) * (nr12 & 0x07);
		}
	}

	private final void updateAudio1()
	{
		if ((nr14 & 0x40) != 0) {
			if (audio1Length > 0) {
				audio1Length--;
			
				if (audio1Length <= 0)
					nr52 &= ~0x01;
			}
		}
		
		if (audio1EnvelopeLength > 0) {
			audio1EnvelopeLength--;
			
			if (audio1EnvelopeLength <= 0) {
				if ((nr12 & 0x08) != 0) {
					if (audio1Volume < 15)
						audio1Volume++;
				}
				else {
					if (audio1Volume > 0)
						audio1Volume--;
				}
				audio1EnvelopeLength += (SOUND_CLOCK / 64) * (nr12 & 0x07);
			}
		}
		
		if (audio1SweepLength > 0) {
			audio1SweepLength--;

			if (audio1SweepLength <= 0) {
				int sweepSteps = (nr10 & 0x07);
				
				if (sweepSteps != 0) {
					int frequency = ((nr14 & 0x07) << 8) + nr13;
					
					if ((nr10 & 0x08) != 0) {
						frequency -= frequency >> sweepSteps;
					}
					else {
						frequency += frequency >> sweepSteps;
					}

					if (frequency < 2048) {
						audio1Frequency = frequencyTable[frequency];
						
						nr13 = frequency & 0xFF;
						nr14 = (nr14 & 0xF8) + ((frequency >> 8) & 0x07);
					}
					else {
						audio1Frequency = 0;
						nr52 &= ~0x01;
					}
				}
				
				audio1SweepLength += (SOUND_CLOCK / 128) * ((nr10 >> 4) & 0x07);
			}
		}
	}
	
	private final void mixAudio1(byte[] buffer, int length)
	{
		int wavePattern = ((nr11 & 0xC0) == 0x00 ? 0x04 :
			   			   (nr11 & 0xC0) == 0x40 ? 0x08 :
			   			   (nr11 & 0xC0) == 0x80 ? 0x10 : 0x18) << 22;
		
		for (int index = 0; index < length; index += 2) {
			audio1Index += audio1Frequency;

			if ((audio1Index & (0x1F << 22)) >= wavePattern) {
				if ((nr51 & 0x10) != 0)
					buffer[index + 0] -= audio1Volume;
				if ((nr51 & 0x01) != 0)
					buffer[index + 1] -= audio1Volume;
			}
			else {
				if ((nr51 & 0x10) != 0)
					buffer[index + 0] += audio1Volume;
				if ((nr51 & 0x01) != 0)
					buffer[index + 1] += audio1Volume;
			}
		}
	}
	
	/*
	 * Audio Channel 2
	 */
	private final int getAudio2Length()
	{
		return nr21;
	}

	private final int getAudio2Envelope()
	{
		return nr22;
	}

	private final int getAudio2Frequency()
	{
		return nr23;
	}

	private final int getAudio2Playback()
	{
		return nr24;
	}

	private final void setAudio2Length(int data)
	{
		nr21 = data;
		
		audio2Length = (SOUND_CLOCK / 256) * (64 - (nr21 & 0x3F));
	}
	
	private final void setAudio2Envelope(int data)
	{
		nr22 = data;
		
		if ((nr24 & 0x40) == 0) {
			if ((nr22 >> 4) == 0) {
				audio2Volume = 0;
			}
			else {
				if (audio2EnvelopeLength == 0 && (nr22 & 0x07) == 0)
					audio2Volume = (audio2Volume + 1) & 0x0F;
				else
					audio2Volume = (audio2Volume + 2) & 0x0F;
			}
		}
	}
	
	private final void setAudio2Frequency(int data)
	{
		nr23 = data;
		
		audio2Frequency = frequencyTable[nr23 + ((nr24 & 0x07) << 8)];
	}
	
	private final void setAudio2Playback(int data)
	{
		nr24 = data;

		audio2Frequency = frequencyTable[nr23 + ((nr24 & 0x07) << 8)];

		if ((nr24 & 0x80) != 0) {
			nr52 |= 0x02;

			if ((nr24 & 0x40) != 0 && audio2Length == 0)
				audio2Length = (SOUND_CLOCK / 256) * (64 - (nr21 & 0x3F));
			
			audio2Volume = nr22 >> 4;
			audio2EnvelopeLength = (SOUND_CLOCK / 64) * (nr22 & 0x07);
		}
	}

	private final void updateAudio2()
	{
		if ((nr24 & 0x40) != 0) {
			if (audio2Length > 0) {
				audio2Length--;
		
				if (audio2Length <= 0)
					nr52 &= ~0x02;
			}
		}

		if (audio2EnvelopeLength > 0) {
			audio2EnvelopeLength--;

			if (audio2EnvelopeLength <= 0) {
				if ((nr22 & 0x08) != 0) {
					if (audio2Volume < 15)
						audio2Volume++;
				}
				else {
					if (audio2Volume > 0)
						audio2Volume--;
				}
				audio2EnvelopeLength += (SOUND_CLOCK / 64) * (nr22 & 0x07);
			}
		}
	}

	private final void mixAudio2(byte[] buffer, int length)
	{
		int wavePattern = ((nr21 & 0xC0) == 0x00 ? 0x04 :
						   (nr21 & 0xC0) == 0x40 ? 0x08 :
						   (nr21 & 0xC0) == 0x80 ? 0x10 : 0x18) << 22;
		
		for (int index = 0; index < length; index += 2) {
			audio2Index += audio2Frequency;

			if ((audio2Index & (0x1F << 22)) >= wavePattern) {
				if ((nr51 & 0x20) != 0)
					buffer[index + 0] -= audio2Volume;
				if ((nr51 & 0x02) != 0)
					buffer[index + 1] -= audio2Volume;
			}
			else {
				if ((nr51 & 0x20) != 0)
					buffer[index + 0] += audio2Volume;
				if ((nr51 & 0x02) != 0)
					buffer[index + 1] += audio2Volume;
			}
		}
	}
	
	/*
	 * Audio Channel 3
	 */
	private final int getAudio3Enable()
	{
		return nr30;
	}

	private final int getAudio3Length()
	{
		return nr31;
	}

	private final int getAudio3Level()
	{
		return nr32;
	}

	private final int getAudio4Frequency()
	{
		return nr33;
	}

	private final int getAudio3Playback()
	{
		return nr34;
	}

	private final void setAudio3Enable(int data)
	{
		nr30 = data & 0x80;
		
		if ((nr30 & 0x80) == 0)
			nr52 &= ~0x04;
	}

	private final void setAudio3Length(int data)
	{
		nr31 = data;
		
		audio3Length = (SOUND_CLOCK / 256) * (256 - nr31);
	}

	private final void setAudio3Level(int data)
	{
		nr32 = data;
	}

	private final void setAudio3Frequency(int data)
	{
		nr33 = data;
		
		audio3Frequency = frequencyTable[((nr34 & 0x07) << 8) + nr33] >> 1;
	}

	private final void setAudio3Playback(int data)
	{
		nr34 = data;
		
		audio3Frequency = frequencyTable[((nr34 & 0x07) << 8) + nr33] >> 1;
		
		if ((nr34 & 0x80) != 0 && (nr30 & 0x80) != 0) {			
			nr52 |= 0x04;

			if ((nr34 & 0x40) != 0 && audio3Length == 0)
				audio3Length = (SOUND_CLOCK / 256) * (256 - nr31);
		}
	}

	private final void setAudio3WavePattern(int address, int data)
	{
		audio3WavePattern[address & 0x0F] = (byte) data;
	}
	
	private final int getAudio3WavePattern(int address)
	{
		return audio3WavePattern[address & 0x0F] & 0xFF;
	}
	
	private final void updateAudio3()
	{
		if ((nr34 & 0x40) != 0) {
			if (audio3Length > 0) {
				audio3Length--;
				
				if (audio3Length <= 0)
					nr52 &= ~0x04;
			}
		}
	}
	
	private final void mixAudio3(byte[] buffer, int length)
	{
		int level = ((nr32 & 0x60) == 0x00 ? 8 :
					 (nr32 & 0x60) == 0x20 ? 0 :
					 (nr32 & 0x60) == 0x40 ? 1 : 2);
		
		for (int index = 0; index < length; index += 2) {
			audio3Index += audio3Frequency;
			
			int sample = audio3WavePattern[(audio3Index >> 23) & 0x0F];
			
			if ((audio3Index & (1 << 22)) != 0)
				sample = (sample >> 0) & 0x0F;
			else
				sample = (sample >> 4) & 0x0F;
			
			sample = ((sample - 8) << 1) >> level;
		
			if ((nr51 & 0x40) != 0)
				buffer[index + 0] += sample;
			if ((nr51 & 0x04) != 0)
				buffer[index + 1] += sample;
		}
	}
	
	/*
	 * Audio Channel 4
	 */
	private final int getAudio4Length()
	{
		return nr41;
	}

	private final int getAudio4Envelope()
	{
		return nr42;
	}

	private final int getAudio4Polynomial()
	{
		return nr43;
	}

	private final int getAudio4Playback()
	{
		return nr44;
	}
	
	private final void setAudio4Length(int data)
	{
		nr41 = data;
		
		audio4Length = (SOUND_CLOCK / 256) * (64 - (nr41 & 0x3F));
	}
	
	private final void setAudio4Envelope(int data)
	{
		nr42 = data;
		
		if ((nr44 & 0x40) == 0) {
			if ((nr42 >> 4) == 0) {
				audio4Volume = 0;
			}
			else {
				if (audio4EnvelopeLength == 0 && (nr42 & 0x07) == 0)
					audio4Volume = (audio4Volume + 1) & 0x0F;
				else
					audio4Volume = (audio4Volume + 2) & 0x0F;
			}
		}
	}

	private final void setAudio4Polynomial(int data)
	{
		nr43 = data;

		if ((nr43 >> 4) <= 12) {
			audio4Frequency = noiseFreqRatioTable[nr43 & 0x07] >> ((nr43 >> 4) + 1);
		}
		else {
			audio4Frequency = 0;
		}
	}

	private final void setAudio4Playback(int data)
	{
		nr44 = data;
		
		if((nr44 & 0x80) != 0) {
			nr52 |= 0x08;

			if ((nr44 & 0x40) != 0 && audio4Length == 0) {
				audio4Length = (SOUND_CLOCK / 256) * (64 - (nr41 & 0x3F));
			}
			
			audio4Volume = nr42 >> 4;
			audio4EnvelopeLength = (SOUND_CLOCK / 64) * (nr42 & 0x07);

			audio4Index = 0;
		}
	}

	private final void updateAudio4()
	{
		if ((nr44 & 0x40) != 0) {
			if (audio4Length > 0) {
				audio4Length--;

				if (audio4Length <= 0)
					nr52 &= ~0x08;
			}
		}

		if (audio4EnvelopeLength > 0) {
			audio4EnvelopeLength--;

			if (audio4EnvelopeLength <= 0) {
				if ((nr42 & 0x08) != 0) {
					if (audio4Volume < 15)
						audio4Volume++;
				}
				else {
					if (audio4Volume > 0)
						audio4Volume--;
				}

				audio4EnvelopeLength += (SOUND_CLOCK / 64) * (nr42 & 0x07);
			}
		}
	}

	private final void mixAudio4(byte[] buffer, int length)
	{
		for (int index = 0; index < length; index += 2) {
			audio4Index += audio4Frequency;
			
			int polynomial;
			
			if ((nr43 & 0x08) != 0) {
				// 7 steps
				audio4Index &= 0x7FFFFF;
				polynomial = noiseStep7Table[audio4Index >> 21] >> ((audio4Index >> 16) & 31);
			}
			else {
				// 15 steps
				audio4Index &= 0x7FFFFFFF;
				polynomial = noiseStep15Table[audio4Index >> 21] >> ((audio4Index >> 16) & 31);
			}
	
			if ((polynomial & 1) != 0) {
				if ((nr51 & 0x80) != 0)
					buffer[index + 0] -= audio4Volume;
				if ((nr51 & 0x08) != 0)
					buffer[index + 1] -= audio4Volume;
			}
			else {
				if ((nr51 & 0x80) != 0)
					buffer[index + 0] += audio4Volume;
				if ((nr51 & 0x08) != 0)
					buffer[index + 1] += audio4Volume;
			}
		}
	}
	
	/*
	 * Output Control
	 */
	private final int getOutputLevel()
	{
		return nr50;
	}

	private final int getOutputTerminal()
	{
		return nr51;
	}

	private final int getOutputEnable()
	{
		return nr52;
	}

	private final void setOutputLevel(int data)
	{
		nr50 = data;
	}

	private final void setOutputTerminal(int data)
	{
		nr51 = data;
	}

	private final void setOutputEnable(int data)
	{
		nr52 = (nr52 & 0x7F) | (data & 0x80);
		
		if ((nr52 & 0x80) == 0x00)
			nr52 &= 0xF0;
	}

	/*
	 * Frequency Table Generation
	 */
	private final void generateFrequencyTables()
	{
		int sampleRate = driver.getSampleRate();
		
		/*
		 * frequency = (4194304 / 32) / (2048 - period) Hz
		 */
		for (int period = 0; period < 2048; period++) {	
			int skip = (((GAMEBOY_CLOCK << 10) / sampleRate) << (22 - 8)) / (2048 - period);

			if (skip >= (32 << 22))
				frequencyTable[period] = 0;
			else
				frequencyTable[period] = skip;
		}
		
		/*
		 * Polynomial Noise Frequency Ratios
		 * 
		 * 4194304 Hz * 1/2^3 * 2 
		 * 4194304 Hz * 1/2^3 * 1 
		 * 4194304 Hz * 1/2^3 * 1/2 
		 * 4194304 Hz * 1/2^3 * 1/3
		 * 4194304 Hz * 1/2^3 * 1/4
		 * 4194304 Hz * 1/2^3 * 1/5
		 * 4194304 Hz * 1/2^3 * 1/6
		 * 4194304 Hz * 1/2^3 * 1/7
		 */
		for (int ratio = 0; ratio < 8; ratio++) {
			noiseFreqRatioTable[ratio] = (GAMEBOY_CLOCK / (ratio == 0 ? 1 : 2 * ratio)) * ((1 << 16) / sampleRate);
		}
	}
	
	/*
	 * Noise Generation
	 */
	private final void generateNoiseTables()
	{
		// 7 steps
		for (int polynomial = 0x7F, index = 0; index <= 0x7F; index++) {
			polynomial = (((polynomial << 6) ^ (polynomial << 5)) & 0x40) | (polynomial >> 1);
			
			if ((index & 31) == 0)
				noiseStep7Table[index >> 5] = 0;

			noiseStep7Table[index >> 5] |= (polynomial & 1) << (index & 31); 
		}
		
		// 15 steps
		for (int polynomial = 0x7FFF, index = 0; index <= 0x7FFF; index++) {
			polynomial = (((polynomial << 14) ^ (polynomial << 13)) & 0x4000) | (polynomial >> 1);
			
			if ((index & 31) == 0)
				noiseStep15Table[index >> 5] = 0;

			noiseStep15Table[index >> 5] |= (polynomial & 1) << (index & 31);
		}
	}
}

/**
 * Mario GameBoy (TM) Emulator
 * 
 * J2ME MIDP 2.0 Emulator Application
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

import org.cafeboy.mario.GameView;

import gameboy.core.Cartridge;
import gameboy.core.GameBoy;

public class Emulator implements Runnable {
	private static final int GAMEBOY_CLOCK = 1 << 20;

	private static final int TIMER_CLOCK = 1000;

	private Video video;
	private Sound sound;
	private Joypad joypad;
	private Store store;
	private Clock clock;
	private GameBoy gameboy;
	private GameView view;
	
	private Thread thread;
	private volatile boolean quit;

	public Emulator(GameView view)
	{
		this.view = view;
		
		video = new Video(view, 160, 144);
		sound = new Sound(44100, 2, 8);
		joypad = new Joypad();
		store = new Store();
		clock = new Clock();

		view.setOnKeyListener(joypad);

		view.setOnTouchListener(joypad);
		gameboy = new GameBoy(video, sound, joypad, store, clock);
		
		thread = new Thread(this);
	}
	
	public void setFrameSkip(int frameSkip)
	{
		gameboy.setFrameSkip(frameSkip);
	}

	public final Cartridge getCartridge()
	{
		return gameboy.getCartridge();
	}
	
	public void load(String cartridgeName)
	{
		gameboy.load(cartridgeName);
	}
	
	public void save(String cartridgeName)
	{
		gameboy.save(cartridgeName);
	}

	public void start()
	{
		if (!thread.isAlive()) {
			quit = false;
			thread.start();
		}
	}
	
	public void stop()
	{
		if (thread.isAlive()) {
			quit = true;
			try {
				thread.join();
			}
			catch (InterruptedException exception) {
			}
		}
	}

	public void run()
	{
		gameboy.reset();

		gameboy.start();
		
		while (!quit) {
			gameboy.emulate(GAMEBOY_CLOCK >> 2);
		}

		gameboy.stop();
	}

}

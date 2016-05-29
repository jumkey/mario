/**
 * Mario GameBoy (TM) Emulator
 * 
 * J2ME MIDP 2.0 Sound Driver
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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import gameboy.core.driver.SoundDriver;

public class Sound implements SoundDriver {
	private int sampleRate;
	private int channels;
	private int bitsPerSample;

	/**
	 * plays the generated .wav data
	 */
	private AudioTrack mAudioTrack;
	public Sound(int sampleRate, int channels, int bitsPerSample)
	{
		this.sampleRate = sampleRate;
		this.channels = channels;
		this.bitsPerSample = bitsPerSample;


		// 获得构建对象的最小缓冲区大小
		int minBufSize = AudioTrack.getMinBufferSize(this.getSampleRate(),
				AudioFormat.CHANNEL_OUT_STEREO,//producer.getChannels(),CHANNEL_OUT_MONO
				AudioFormat.ENCODING_PCM_8BIT);

Log.d("TAG","ss="+minBufSize);

//               STREAM_ALARM：警告声
//               STREAM_MUSIC：音乐声，例如music等
//               STREAM_RING：铃声
//               STREAM_SYSTEM：系统声音
//               STREAM_VOCIE_CALL：电话声音
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				this.getSampleRate(),
				AudioFormat.CHANNEL_OUT_STEREO,//producer.getChannels(),
				AudioFormat.ENCODING_PCM_8BIT,//producer.getBitsPerSample(),
				minBufSize,
				AudioTrack.MODE_STREAM);
//              AudioTrack中有MODE_STATIC和MODE_STREAM两种分类。
//              STREAM的意思是由用户在应用程序通过write方式把数据一次一次得写到audiotrack中。
//              这个和我们在socket中发送数据一样，应用层从某个地方获取数据，例如通过编解码得到PCM数据，然后write到audiotrack。
//              这种方式的坏处就是总是在JAVA层和Native层交互，效率损失较大。
//              而STATIC的意思是一开始创建的时候，就把音频数据放到一个固定的buffer，然后直接传给audiotrack，
//              后续就不用一次次得write了。AudioTrack会自己播放这个buffer中的数据。
//              这种方法对于铃声等内存占用较小，延时要求较高的声音来说很适用。
	}
	
	public boolean isEnabled()
	{
		return true;
	}
	
	public int getBitsPerSample()
	{
		return bitsPerSample;
	}

	public int getChannels()
	{
		return channels;
	}

	public int getSampleRate()
	{
		return sampleRate;
	}

	public void start()
	{


		mAudioTrack.play();


	}

	public void stop()
	{
		this.mAudioTrack.stop();
		this.mAudioTrack.release();
	}

	public void write(byte[] buffer, int length)
	{
		mAudioTrack.write(buffer, 0, length);
		mAudioTrack.flush();;
	}
}

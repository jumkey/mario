/**
 * Mario GameBoy (TM) Emulator
 *
 * J2ME MIDP 2.0 Video Display Driver
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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import org.cafeboy.mario.GameView;

import gameboy.core.driver.VideoDriver;

public class Video implements VideoDriver {
	private GameView view;
	private int[] pixels;
	private int width;
	private int height;

	public Video(GameView view, int width, int height)
	{
		this.view = view;
		this.width = width;
		this.height = height;

		pixels = new int[width * height];

		for (int offset = 0; offset < pixels.length; offset++)
			pixels[offset] = 0;
	}

	public int getWidth()
	{
		return width;
	}

	public int getHeight()
	{
		return height;
	}

	public int[] getPixels()
	{
		return pixels;
	}

	public void display()
	{
		Canvas canvas = view.getHolder().lockCanvas(); // 获取并锁定canvas
		if(canvas!=null) {
			canvas.save();    // 保存当前绘图环境

			// 具体绘制
			Paint paint = new Paint();
			paint.setColor(0);
			paint.setAlpha(255);
			canvas.drawPaint(paint);
			Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.RGB_565);
			canvas.drawBitmap(bitmap, 0, 0, paint);
			//canvas.drawBitmap(pixels, 0, width, 0, 0, width, height, false, paint);

			//放大
			// 定义矩阵对象
			Matrix matrix = new Matrix();
			// 缩放原图
			matrix.postScale(4f, 4f);
			//bmp.getWidth(), bmp.getHeight()分别表示缩放后的位图宽高
			Bitmap dstbmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
			// 在画布上绘制旋转后的位图
			//放在坐标为60,460的位置
			canvas.drawBitmap(dstbmp, 160, 144, paint);


			// show Gameboy buttons if these were created
			if(this.view.getButtons() != null) {
				for(int i = 0, to = this.view.getButtons().length; i < to; ++i) {
					this.view.getButtons()[i].draw(canvas);
				}
			}


			canvas.restore(); // 恢复先前绘图环境
			view.getHolder().unlockCanvasAndPost(canvas); // 解锁canvas并绘制
		}
	}

}

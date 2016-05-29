package org.cafeboy.mario;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import gameboy.platform.android.Emulator;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    //游戏屏幕宽和高
    public static int screen_width, screen_height;
    private SurfaceHolder sfh;
    /**
     * resource ids of the Gameboy buttons
     */
    private final static int[] BUTTON_RESOURCE_IDS = {R.drawable.button_a, R.drawable.button_b, R.drawable.button_select, R.drawable.button_start};
    /**
     * keys assigned to the Gameboy buttons
     */
    private final static int[] BUTTON_KEYS = {KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE};

    public GameView(Context context) {
        super(context);
        sfh = getHolder();
        sfh.addCallback(this);

        //获得焦点
        setFocusable(true);
        setClickable(true);
        setFocusableInTouchMode(true);
        //保持屏幕常亮
        setKeepScreenOn(true);


        // initialize Gameboy buttons if we have a touch screen available
        final Configuration config = context.getResources().getConfiguration();

        config.setToDefaults();
        if (config.touchscreen != Configuration.TOUCHSCREEN_NOTOUCH) {
            this.buttons = new BitmapDrawable[BUTTON_RESOURCE_IDS.length];
            for (int i = 0; i < this.buttons.length; ++i) {
                this.buttons[i] = new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), BUTTON_RESOURCE_IDS[i]));
            }
        } else {
            this.buttons = null;
        }
    }


    private static final String CARTRIDGE_NAME = "/assets/chipthechick.gb";

    private static final int FRAME_SKIP = 5;

    private Emulator emulator;

    /**
     * Gameboy buttons to be displayed when we have a touch screen available
     */
    private final BitmapDrawable buttons[];

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // we need to display additional Gameboy buttons?
        if (this.buttons != null) {
            // ensure that the screen leaves enough space
            final int bw = this.buttons[0].getBitmap().getWidth();
            final int bh = this.buttons[0].getBitmap().getHeight();


            // determine the button positions
            final int n = this.buttons.length;

            // place buttons at the bottom of the screen
            final int xinc = (w - n * bw) / (n - 1) + bw;

            for (int i = 0, x = 0, y = h - bh; i < n; ++i, x += xinc) {
                this.buttons[i].setBounds(x, y, x + bw, y + bh);
            }

        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screen_width = getWidth();
        screen_height = getHeight();


        emulator = new Emulator(this);

        //emulator.setFrameSkip(FRAME_SKIP);

        emulator.load(CARTRIDGE_NAME);
        println("");
        println("Title: " + emulator.getCartridge().getTitle() + " Type: " + emulator.getCartridge().getDescription() + " ROM: " + (emulator.getCartridge().getROMSize() >> 10) + "KB RAM: " + (emulator.getCartridge().getRAMSize() >> 10) + "KB");

        if (!emulator.getCartridge().verify()) {
            println("");
            println("WARNING: Cartridge checksum verification failed");
        }
        emulator.start();

    }

    private static void println(String message) {
        System.out.println(message);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        emulator.stop();
        emulator = null;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public BitmapDrawable[] getButtons() {
        return buttons;
    }

    public static int[] getButtonKeys() {
        return BUTTON_KEYS;
    }
}

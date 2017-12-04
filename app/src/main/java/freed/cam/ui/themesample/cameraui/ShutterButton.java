/*
 *
 *     Copyright (C) 2015 Ingo Fuchs
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * /
 */

package freed.cam.ui.themesample.cameraui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.troop.freedcam.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import freed.cam.apis.basecamera.CameraWrapperInterface;
import freed.cam.apis.basecamera.modules.ModuleHandlerAbstract;
import freed.cam.apis.basecamera.modules.ModuleHandlerAbstract.CaptureStates;
import freed.cam.ui.themesample.handler.UserMessageHandler;
import freed.settings.AppSettingsManager;
import freed.utils.Log;

/**
 * Created by troop on 20.06.2015.
 */
public class ShutterButton extends android.support.v7.widget.AppCompatButton implements ModuleHandlerAbstract.CaptureStateChanged {
    private CameraWrapperInterface cameraUiWrapper;

    private final String TAG = ShutterButton.class.getSimpleName();
    private CaptureStates currentShow = CaptureStates.image_capture_stop;
    protected HandlerThread mBackgroundThread;
    protected AnimationHandler animationHandler;
    private UIHandler uiHandler;

    public final int MSG_START_ANIMATION = 0;
    public final int MSG_PUBLISHPROGRESS = 1;
    public final int MSG_INVALIDATE = 2;

    private String shutteropentime = "";


    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("ShutterDraw");
        mBackgroundThread.start();
        animationHandler = new AnimationHandler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread()
    {
        Log.d(TAG, "stopShutterDraw");
        if (mBackgroundThread == null)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBackgroundThread.quitSafely();
        } else
            mBackgroundThread.quit();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            animationHandler = null;
        } catch (InterruptedException e) {
            Log.WriteEx(e);
        }
    }

    private class UIHandler extends Handler
    {
        public UIHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what)
            {
                case MSG_INVALIDATE:
                    ShutterButton.this.invalidate();
                    break;
                case MSG_PUBLISHPROGRESS:
                   publishProgress((String)msg.obj);
                   break;
                default:
                    super.handleMessage(msg);
                    break;
            }

        }

        private void publishProgress(String s)
        {
            shutteropentime = s;
            ShutterButton.this.invalidate();
        }
    }

    private class AnimationHandler extends Handler
    {

        int red_top, red_bottom, red_right,red_left, padding_red, red_radius, halfsize;
        float txt_left, txt_right,txt_top,txt_bottom, txt_length,txt_height;

        //shutter_open_radius for the Transparent Radius to draw to simulate shutter open
        private float shutter_open_radius = 0.0f;
        //true when the red recording button should get shown, used for continouse capture and video
        private boolean drawRecordingImage = false;
        //current size of the red circle to draw
        private int recordingRadiusCircle;
        //current size of the red rectangle to draw
        private int recordingRadiusRectangle;

        //holds the time from a capture start
        private long startime;
        //frames to draw
        private final int MAXFRAMES = 10;
        //holds the currentframe number
        private int currentframe = 0;

        private boolean running = false;

        private boolean stopTimer = false;
        private int MAX_SHUTTER_OPEN;
        private int MAX_RECORDING_OPEN;
        private int RECORDING_OPEN_STEP;

        //the step wich the shutter_open_radius gets increased/decrased
        private int SHUTTER_OPEN_STEP;
        private boolean shutteractive = false;

        private boolean drawTimer = false;
        private Paint shutteropentimePaint;




        private Paint transparent;
        private Paint red;

        public boolean isRunning()
        {
            return running;
        }

        private SimpleDateFormat dateFormat = new SimpleDateFormat("mm:ss:SSS");

        private final int FPS = 1000/ 30;

        public AnimationHandler(Looper looper)
        {
            super(looper);
            //used to draw green timer inside the shutter button
            shutteropentimePaint = new Paint();
            shutteropentimePaint.setColor(Color.GREEN);
            shutteropentimePaint.setTextSize(getResources().getDimension(R.dimen.cameraui_infooverlay_textsize));
            shutteropentimePaint.setStyle(Paint.Style.FILL);
            shutteropentimePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
            shutteropentimePaint.setAntiAlias(true);

            //used to open the shutter
            transparent = new Paint();
            transparent.setColor(Color.TRANSPARENT);
            transparent.setStyle(Paint.Style.FILL);
            transparent.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            transparent.setAntiAlias(true);

            //used to for the recording button
            red = new Paint();
            red.setColor(Color.RED);
            red.setStyle(Paint.Style.FILL);
            red.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
            red.setAntiAlias(true);
        }

        private void startAnimation()
        {
            this.obtainMessage(MSG_START_ANIMATION).sendToTarget();
        }

        public void startDrawing() {
            shutteractive = true;
            stopTimer = false;
            if (isRunning()) {
                setStartTime(System.currentTimeMillis());
            }
            else {
                setStartTime(System.currentTimeMillis());
                startAnimation();
            }
        }

        public void drawTimer(boolean drawTimer)
        {
            synchronized (this) {
                this.drawTimer = drawTimer;
            }
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what)
            {
                case MSG_START_ANIMATION:
                    if (!isRunning())
                        run();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

        public void setStartTime(long startTime)
        {
            synchronized (this)
            {
                this.startime = startTime;
            }
        }

        private void stopShutterTimer() {
            synchronized (this) {
                stopTimer = true;
            }
        }


        private long calcstartTime;

        private void run()
        {
            calcstartTime = System.nanoTime();
            halfsize = getWidth()/2;
            MAX_SHUTTER_OPEN = (getWidth() - 100) / 2;
            SHUTTER_OPEN_STEP = (MAX_SHUTTER_OPEN) / MAXFRAMES;
            MAX_RECORDING_OPEN = getWidth() /4;
            RECORDING_OPEN_STEP = MAX_RECORDING_OPEN/MAXFRAMES;

            int recordingSize = MAX_RECORDING_OPEN;
            recordingRadiusCircle = recordingSize;
            recordingRadiusRectangle = 0;
            running = true;
            while (shutteractive)
            {
                if (currentframe < MAXFRAMES)
                    draw();
                else {
                    draw();
                    currentframe = 0;
                    if (stopTimer) {
                        uiHandler.obtainMessage(MSG_PUBLISHPROGRESS,"").sendToTarget();
                        shutteractive = false;
                    }
                }
                if (drawTimer)
                    uiHandler.obtainMessage(MSG_PUBLISHPROGRESS,getTimeGoneString(startime)).sendToTarget();
                else
                    uiHandler.obtainMessage(MSG_PUBLISHPROGRESS,"").sendToTarget();
                currentframe++;
                try {
                    long sleep = FPS-((System.nanoTime()-calcstartTime)/1000000L);
                    if (sleep > 0)
                        Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Log.WriteEx(e);
                }
            }
            running = false;

            shutteropentime = "";
            uiHandler.obtainMessage(MSG_INVALIDATE).sendToTarget();
        }



        private String getTimeGoneString(long startime)
        {
            long now = System.currentTimeMillis();
            long dif = now - startime;
            dateFormat.setTimeZone(TimeZone.getDefault());
            return dateFormat.format(new Date(dif));

        }

        private void draw() {
            synchronized (this) {
                switch (currentShow) {
                    case video_recording_stop:
                        shutter_open_radius = 0;
                        recordingRadiusCircle += RECORDING_OPEN_STEP;
                        if (recordingRadiusCircle > MAX_RECORDING_OPEN || currentframe == MAXFRAMES)
                            recordingRadiusCircle = MAX_RECORDING_OPEN;

                        recordingRadiusRectangle -= RECORDING_OPEN_STEP;
                        if (recordingRadiusRectangle < 0 || currentframe == MAXFRAMES)
                            recordingRadiusRectangle = 0;
                        drawRecordingImage = true;
                        break;
                    case video_recording_start:
                        shutter_open_radius = 0;
                        recordingRadiusCircle -= RECORDING_OPEN_STEP;
                        if (recordingRadiusCircle < 0 || currentframe == MAXFRAMES)
                            recordingRadiusCircle = 0;

                        recordingRadiusRectangle += RECORDING_OPEN_STEP;
                        if (recordingRadiusRectangle > MAX_RECORDING_OPEN || currentframe == MAXFRAMES)
                            recordingRadiusRectangle = MAX_RECORDING_OPEN;
                        drawRecordingImage = true;
                        break;
                    case image_capture_stop:
                        drawRecordingImage = false;
                        shutter_open_radius -= SHUTTER_OPEN_STEP;
                        if (shutter_open_radius < 0 || currentframe == MAXFRAMES)
                            shutter_open_radius = 0;
                        break;
                    case image_capture_start:
                        drawRecordingImage = false;
                        shutter_open_radius += SHUTTER_OPEN_STEP;
                        if (shutter_open_radius > MAX_SHUTTER_OPEN || currentframe == MAXFRAMES)
                            shutter_open_radius = MAX_SHUTTER_OPEN;
                        break;
                    case continouse_capture_start:
                        drawRecordingImage = true;
                        shutter_open_radius += SHUTTER_OPEN_STEP;
                        if (shutter_open_radius > MAX_SHUTTER_OPEN || currentframe == MAXFRAMES)
                            shutter_open_radius = MAX_SHUTTER_OPEN;

                        recordingRadiusCircle -= RECORDING_OPEN_STEP;
                        if (recordingRadiusCircle < 0 || currentframe == MAXFRAMES)
                            recordingRadiusCircle = 0;
                        recordingRadiusRectangle += RECORDING_OPEN_STEP;
                        if (recordingRadiusRectangle > MAX_RECORDING_OPEN || currentframe == MAXFRAMES)
                            recordingRadiusRectangle = MAX_RECORDING_OPEN;
                        break;
                    case cont_capture_stop_while_working:
                        drawRecordingImage = true;
                        //shutter_open_radius += SHUTTER_OPEN_STEP;
                        recordingRadiusCircle += RECORDING_OPEN_STEP;
                        if (recordingRadiusCircle > MAX_RECORDING_OPEN || currentframe == MAXFRAMES)
                            recordingRadiusCircle = MAX_RECORDING_OPEN;
                        recordingRadiusRectangle -= RECORDING_OPEN_STEP;
                        if (recordingRadiusRectangle < 0 || currentframe == MAXFRAMES)
                            recordingRadiusRectangle = 0;
                        break;
                    case cont_capture_stop_while_notworking:
                        shutter_open_radius = 0;
                        recordingRadiusCircle += RECORDING_OPEN_STEP;
                        if (recordingRadiusCircle > MAX_RECORDING_OPEN || currentframe == MAXFRAMES)
                            recordingRadiusCircle = MAX_RECORDING_OPEN;
                        recordingRadiusRectangle -= RECORDING_OPEN_STEP;
                        if (recordingRadiusRectangle < 0 || currentframe == MAXFRAMES)
                            recordingRadiusRectangle = 0;
                        drawRecordingImage = true;
                        break;
                    case continouse_capture_stop:
                        recordingRadiusCircle += RECORDING_OPEN_STEP;
                        if (recordingRadiusCircle > MAX_RECORDING_OPEN || currentframe == MAXFRAMES)
                            recordingRadiusCircle = MAX_RECORDING_OPEN;
                        recordingRadiusRectangle -= RECORDING_OPEN_STEP;
                        if (recordingRadiusRectangle < 0 || currentframe == MAXFRAMES)
                            recordingRadiusRectangle = 0;
                        drawRecordingImage = true;
                        break;
                    case continouse_capture_work_start:
                        drawRecordingImage = true;
                        shutter_open_radius += SHUTTER_OPEN_STEP;
                        if (shutter_open_radius > MAX_SHUTTER_OPEN)
                            shutter_open_radius = MAX_SHUTTER_OPEN;

                        break;
                    case continouse_capture_work_stop:
                        drawRecordingImage = true;
                        shutter_open_radius -= SHUTTER_OPEN_STEP;
                        if (shutter_open_radius < 0 || currentframe == MAXFRAMES)
                            shutter_open_radius = 0;
                        break;
                }

                padding_red = halfsize;
                red_radius = recordingRadiusRectangle / 2;
                if (drawRecordingImage) {
                    red_top = padding_red - red_radius;
                    red_bottom = padding_red + red_radius;
                    red_left = padding_red - red_radius;
                    red_right = padding_red + red_radius;
                    padding_red = red_bottom + red_radius + space;
                }
                if (drawTimer && !TextUtils.isEmpty(shutteropentime)) {
                    txt_length = shutteropentimePaint.measureText(shutteropentime);
                    txt_height = shutteropentimePaint.getTextSize();
                    txt_left = halfsize - (txt_length / 2);
                    txt_top = padding_red - txt_height - space;
                    txt_right = halfsize + (txt_length / 2) + space;
                    txt_bottom = padding_red + txt_height / 2 + space;
                }
            }
            //Log.d(TAG,"shutter_open:" + shutter_open_radius + " recCircle:" + recordingRadiusCircle + " recRect:" + recordingRadiusRectangle +  " captureState:" + currentShow);
        }


        private final int space = 3;

        public void onDraw(Canvas canvas)
        {
            synchronized (this) {
                canvas.drawCircle(halfsize, halfsize, shutter_open_radius, transparent);
                if (drawRecordingImage) {
                    canvas.drawCircle(halfsize, halfsize, recordingRadiusCircle / 2, red);
                    canvas.drawRect(red_left, red_top, red_right, red_bottom, red);

                }
                if (drawTimer && !TextUtils.isEmpty(shutteropentime)) {

                    shutteropentimePaint.setColor(Color.BLACK);
                    shutteropentimePaint.setAlpha(125);

                    canvas.drawRect(txt_left - space, txt_top, txt_right, txt_bottom, shutteropentimePaint);

                    shutteropentimePaint.setColor(Color.GREEN);
                    shutteropentimePaint.setAlpha(255);
                    canvas.drawText(shutteropentime, txt_left, padding_red, shutteropentimePaint);
                }
            }
        }
    }

    public ShutterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(context);
    }

    public ShutterButton(Context context) {
        super(context);
        this.init(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startBackgroundThread();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopBackgroundThread();
    }

    private void init(Context context) {
        uiHandler = new UIHandler(Looper.getMainLooper());


        //set background img that get then overdrawn
        setBackgroundResource(R.drawable.shutter5);

        this.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraUiWrapper == null || cameraUiWrapper.getModuleHandler() == null || cameraUiWrapper.getModuleHandler().getCurrentModule() == null)
                    return;
                String sf = AppSettingsManager.getInstance().selfTimer.get();
                if (TextUtils.isEmpty(sf))
                    sf = "0";
                int selftimer = Integer.parseInt(sf);
                if(selftimer > 0) {
                    uiHandler.postDelayed(selftimerRunner, selftimer*1000);
                    onCaptureStateChanged(CaptureStates.selftimerstart);
                }
                else
                    cameraUiWrapper.getModuleHandler().getCurrentModule().DoWork();
            }
        });

    }

    private Runnable selftimerRunner =new Runnable() {
        @Override
        public void run() {
            onCaptureStateChanged(CaptureStates.selftimerstop);
            cameraUiWrapper.getModuleHandler().getCurrentModule().DoWork();
        }
    };


    public void SetCameraUIWrapper(CameraWrapperInterface cameraUiWrapper, UserMessageHandler messageHandler) {
        if (cameraUiWrapper.getModuleHandler() == null)
            return;
        this.cameraUiWrapper = cameraUiWrapper;
        cameraUiWrapper.getModuleHandler().setWorkListner(this);
        if(cameraUiWrapper.getModuleHandler().getCurrentModule() != null)
            onCaptureStateChanged(cameraUiWrapper.getModuleHandler().getCurrentModule().getCurrentCaptureState());
        Log.d(this.TAG, "Set cameraUiWrapper to ShutterButton");
    }

    @Override
    public void onCaptureStateChanged(CaptureStates mode) {
        if(mode == null) {
            Log.d(TAG, "onCaptureStateChanged: Capture State is null");
            return;
        }
        Log.d(this.TAG, "onCaptureStateChanged CurrentShow:" + this.currentShow + " Capturestate: " + mode.name());
        //first start shutter animation
        currentShow = mode;
        Log.d(TAG, "switchBackground:" + currentShow);
        if (!animationHandler.isRunning())
            animationHandler.startDrawing();

        //set specfic mode overides like drawtimer,
        //setting it that way shutter is already abit opend till the green timer gets visible
        switch (mode) {
            case video_recording_stop:
                animationHandler.drawTimer(false);
                animationHandler.stopShutterTimer();
                break;
            case video_recording_start:
                animationHandler.drawTimer(true);
                break;
            case image_capture_stop:
                animationHandler.drawTimer(false);
                animationHandler.stopShutterTimer();
                break;
            case image_capture_start:
                animationHandler.drawTimer(true);
                break;
            case continouse_capture_start:
                break;
            case continouse_capture_stop:
                break;
            case continouse_capture_work_start:
                animationHandler.drawTimer(true);
                break;
            case continouse_capture_work_stop:
                animationHandler.drawTimer(false);
                break;
            case cont_capture_stop_while_working:
                break;
            case cont_capture_stop_while_notworking:
                animationHandler.drawTimer(false);
                animationHandler.stopShutterTimer();
                break;
            case selftimerstart:
                animationHandler.drawTimer(true);
                break;
            case selftimerstop:
                animationHandler.drawTimer(false);
                animationHandler.stopShutterTimer();
        }

    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        animationHandler.onDraw(canvas);
    }

}
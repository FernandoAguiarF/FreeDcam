package com.troop.freedcamv2.camera.modules;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.troop.freedcamv2.camera.BaseCameraHolder;
import com.troop.freedcamv2.ui.AppSettingsManager;
import com.troop.yuv.Merge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by troop on 27.10.2014.
 */
public class LongExposureModule extends AbstractModule implements Camera.PreviewCallback
{
    public LongExposureModule(BaseCameraHolder cameraHandler, AppSettingsManager Settings, ModuleEventHandler eventHandler) {
        super(cameraHandler, Settings, eventHandler);
        name = ModuleHandler.MODULE_LONGEXPO;
        exposureModule = this;
        frameThread = new HandlerThread(TAG);
        frameThread.start();
        frameHandler = new Handler(frameThread.getLooper());
    }

    private class YuvHolder
    {
        public int y;
        public int u;
        public int v;
    }

    YuvHolder yuvIntHolder[];

    LongExposureModule exposureModule;
    //if true the the preview frames are grabed
    boolean doWork = false;
    //if true a preview frame merge is in progress
    boolean hasWork = false;
    //stores the basyuv data wich get merged with the other frames
    //byte[] baseYuv;
    //stores the actual frame to merge
    byte[] mergeYuv;
    //preview size height
    int height;
    //preview size width
    int width;
    //handler to process the merge
    Handler handler;
    Handler frameHandler;
    HandlerThread frameThread;
    static String TAG = "freedcam.LongExposure";
    int count;

    Merge nativeYuvMerge;


    @Override
    public void DoWork()
    {


        //check if already working if true return
        if (this.isWorking)
            return;
        //set working true
        this.isWorking = true;
        count = 0;
        //get width and height from the preview
        width = baseCameraHolder.ParameterHandler.PreviewSize.GetWidth();
        height = baseCameraHolder.ParameterHandler.PreviewSize.GetHeight();
        if (nativeYuvMerge == null)
            nativeYuvMerge = new Merge();

        //start listen to the previewcallback
        baseCameraHolder.SetPreviewCallback(this);
        //enable frame listing for the callback
        doWork = true;
        //set baseyuv to null to start a new merge
        //baseYuv = null;
        // get the exposure duration
        int time = Integer.parseInt(Settings.getString(AppSettingsManager.SETTING_EXPOSURELONGTIME));
        handler = new Handler();
        //post the runnable after wich time it should stop grabbing the preview frames
        handler.postDelayed(runnableFinishWork, time*1000);

    }

    @Override
    public void onPreviewFrame(final byte[] data, Camera camera)
    {
        //if base yuv null a new
        //if (baseYuv == null)
            //baseYuv = data;
        if (doWork && !hasWork)
        {
            mergeYuv = data;
            if (mergeYuv != null)
            {
                baseCameraHolder.GetCamera().setPreviewCallback(null);
                new Thread(runnable).start();
            }
        }
    }

    //this runs when the time is gone and stops listen to the preview and convert then the yuv data into an bitmap and saves it
    Runnable runnableFinishWork = new Runnable() {
        @Override
        public void run()
        {
            exposureModule.doWork = false;
            while (exposureModule.hasWork) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            baseCameraHolder.GetCamera().setPreviewCallback(null);

            mergeYuv = nativeYuvMerge.GetMergedYuv(count);
            File file = createFilename();
            OutputStream outStream = null;
            try
            {
                outStream = new FileOutputStream(file);
                YuvImage img  = new YuvImage(mergeYuv, ImageFormat.NV21, width, height, null);
                img.compressToJpeg(new Rect(0, 0, width, height), 100, outStream);
                //outStream.write(mergeYuv);
                outStream.flush();
                outStream.close();
                img = null;
                //System.gc();
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            mergeYuv = null;
            nativeYuvMerge.Release();

            Log.d(TAG, "Work done");
            eventHandler.WorkFinished(file);
            exposureModule.isWorking = false;
            System.gc();
        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run()
        {
             processYuvFrame();
        }
    };


    private void processYuvFrame() {
        this.hasWork = true;
        Log.d(TAG, "StartProcessingFrame");
        if (mergeYuv == null)
            return;
        int i = 0;

        int frameSize = width * height;

        if (count == 0)
            nativeYuvMerge.AddFirstYuvFrame(mergeYuv, width, height);
        else
            nativeYuvMerge.AddNextYuvFrame(mergeYuv);

        count++;
        Log.d(TAG, "Frame Processed:" + count);


        this.hasWork = false;
        baseCameraHolder.GetCamera().setPreviewCallback(this);
    }


    private File createFilename() {
        Log.d(TAG,"Create FileName");
        String pictureFormat = baseCameraHolder.ParameterHandler.PictureFormat.GetValue();
        File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/FreeCam/");
        if(!file.exists())
            file.mkdirs();
        Date date = new Date();
        String s = (new SimpleDateFormat("yyyyMMdd_HHmmss")).format(date);
        String s1 = (new StringBuilder(String.valueOf(file.getPath()))).append(File.separator).append("IMG_").append(s).toString();
        return new File((new StringBuilder(String.valueOf(s1))).append(".jpg").toString());
    }
}

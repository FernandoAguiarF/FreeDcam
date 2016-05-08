package com.freedcam.apis.camera1.camera.parameters.modes;

import android.os.Handler;

import com.freedcam.apis.camera1.camera.CameraHolderApi1;

import java.util.HashMap;

/**
 * Created by troop on 21.08.2014.
 */
public class PreviewFpsParameter extends  BaseModeParameter
{
    private CameraHolderApi1 cameraHolder;

    public PreviewFpsParameter(Handler handler, HashMap<String, String> parameters, String values, CameraHolderApi1 holder) {
        super(handler ,parameters, holder, "preview-frame-rate", "preview-frame-rate-values");
        this.cameraHolder = holder;
    }



    @Override
    public void SetValue(String valueToSet, boolean setToCam)
    {
        super.SetValue(valueToSet, setToCam);
        if (setToCam) {
            cameraHolder.StopPreview();
            cameraHolder.StartPreview();
        }
        firststart = false;

    }

    @Override
    public String GetValue() {
        return super.GetValue();
    }

    @Override
    public String[] GetValues() {
        return super.GetValues();
    }
}
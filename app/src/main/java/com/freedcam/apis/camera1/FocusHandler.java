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

package com.freedcam.apis.camera1;

import android.view.MotionEvent;

import com.freedcam.apis.basecamera.AbstractFocusHandler;
import com.freedcam.apis.basecamera.FocusRect;
import com.freedcam.apis.basecamera.interfaces.I_CameraUiWrapper;
import com.freedcam.apis.basecamera.modules.CameraFocusEvent;
import com.freedcam.apis.basecamera.modules.I_Callbacks.AutoFocusCallback;
import com.freedcam.apis.basecamera.parameters.modes.AbstractModeParameter.I_ModeParameterEvent;
import com.freedcam.apis.camera1.CameraHolder.Frameworks;
import com.freedcam.utils.DeviceUtils.Devices;

/**
 * Created by troop on 02.09.2014.
 */
public class FocusHandler extends AbstractFocusHandler implements AutoFocusCallback
{
    final String TAG = FocusHandler.class.getSimpleName();
    private boolean aeMeteringSupported =false;

    public FocusHandler(I_CameraUiWrapper cameraUiWrapper)
    {
        super(cameraUiWrapper);
    }

    public I_ModeParameterEvent focusModeListner = new I_ModeParameterEvent() {
        @Override
        public void onValueChanged(String val)
        {
            if (((CameraHolder)cameraUiWrapper.GetCameraHolder()).DeviceFrameWork != Frameworks.MTK) {
                if (val.equals("auto") || val.equals("macro") || val.equals("touch")) {
                    if (focusEvent != null)
                        focusEvent.TouchToFocusSupported(true);
                } else {
                    if (focusEvent != null)
                        focusEvent.TouchToFocusSupported(false);
                }
            }
            else {
                if (focusEvent != null) {
                    aeMeteringSupported = true;
                    focusEvent.AEMeteringSupported(true);
                }
            }

        }

        @Override
        public void onIsSupportedChanged(boolean isSupported) {

        }

        @Override
        public void onIsSetSupportedChanged(boolean isSupported) {

        }

        @Override
        public void onValuesChanged(String[] values) {

        }

        @Override
        public void onVisibilityChanged(boolean visible) {

        }
    };
    public I_ModeParameterEvent aeModeListner = new I_ModeParameterEvent() {
        @Override
        public void onValueChanged(String val)
        {
            if(((CameraHolder)cameraUiWrapper.GetCameraHolder()).DeviceFrameWork != Frameworks.MTK)
            {
                if (val.contains("spot")) {
                    if (focusEvent != null) {
                        aeMeteringSupported = true;
                        focusEvent.AEMeteringSupported(true);
                    }
                } else {
                    if (focusEvent != null) {
                        aeMeteringSupported = false;
                        focusEvent.AEMeteringSupported(false);
                    }
                }
            }
            else
            {
                if (focusEvent != null) {
                    aeMeteringSupported = true;
                    focusEvent.AEMeteringSupported(true);
                }
            }

        }

        @Override
        public void onIsSupportedChanged(boolean isSupported) {

        }

        @Override
        public void onIsSetSupportedChanged(boolean isSupported) {

        }

        @Override
        public void onValuesChanged(String[] values) {

        }

        @Override
        public void onVisibilityChanged(boolean visible) {

        }
    };



    @Override
    public boolean isAeMeteringSupported()
    {
        return aeMeteringSupported;
    }

    @Override
    public void SetMotionEvent(MotionEvent event) {

    }

    @Override
    public void onAutoFocus(CameraFocusEvent event)
    {
        if (focusEvent != null)
            focusEvent.FocusFinished(event.success);
    }

    @Override
    public void onFocusLock(boolean locked) {

    }
    @Override
    public void StartFocus()
    {
        if (focusEvent != null)
        {
            focusEvent.FocusStarted(null);
        }
        ((CameraHolder)cameraUiWrapper.GetCameraHolder()).StartFocus(this);
    }

    @Override
    public void StartTouchToFocus(FocusRect rect,int width, int height)
    {
        if (cameraUiWrapper == null|| cameraUiWrapper.GetParameterHandler() == null || cameraUiWrapper.GetParameterHandler().FocusMode == null)
            return;

        String focusmode = cameraUiWrapper.GetParameterHandler().FocusMode.GetValue();
        if (focusmode.equals("auto") || focusmode.equals("macro"))
        {
            FocusRect targetFocusRect = getFocusRect(rect, width, height);

            if (targetFocusRect.left >= -1000
                    && targetFocusRect.top >= -1000
                    && targetFocusRect.bottom <= 1000
                    && targetFocusRect.right <= 1000)
            {

                cameraUiWrapper.GetParameterHandler().SetFocusAREA(targetFocusRect);
                if (cameraUiWrapper.GetCameraHolder() != null)
                    ((CameraHolder)cameraUiWrapper.GetCameraHolder()).StartFocus(this);
                if (focusEvent != null)
                    focusEvent.FocusStarted(rect);
            }
        }

    }

    @Override
    public void SetMeteringAreas(FocusRect meteringRect, int width, int height)
    {
        if (cameraUiWrapper.GetAppSettingsManager().getDevice() == Devices.ZTE_ADV)
        {
            FocusRect targetFocusRect = getFocusRect(meteringRect, width, height);
            cameraUiWrapper.GetParameterHandler().SetMeterAREA(targetFocusRect);
        }
        else {
            FocusRect targetFocusRect = getFocusRect(meteringRect, width, height);
            ((CameraHolder)cameraUiWrapper.GetCameraHolder()).SetMeteringAreas(targetFocusRect);
        }

    }

    private FocusRect getFocusRect(FocusRect rect, int width, int height)
    {
        logFocusRect(rect);
        if (width == 0 || height == 0)
            return null;
        FocusRect targetFocusRect = new FocusRect(
                rect.left * 2000 / width - 1000,
                rect.right * 2000 / width - 1000,
                rect.top * 2000 / height - 1000,
                rect.bottom * 2000 / height - 1000,
                rect.x,rect.y);
        logFocusRect(targetFocusRect);
        //check if stuff is to big or to small and set it to min max key_value
        if (targetFocusRect.left < -1000)
        {
            int dif = targetFocusRect.left + 1000;
            targetFocusRect.left = -1000;
            targetFocusRect.right += dif;
        }
        if (targetFocusRect.right > 1000)
        {
            int dif = targetFocusRect.right - 1000;
            targetFocusRect.right = 1000;
            targetFocusRect.left -= dif;
        }
        if (targetFocusRect.top < -1000)
        {
            int dif = targetFocusRect.top + 1000;
            targetFocusRect.top = -1000;
            targetFocusRect.bottom += dif;
        }
        if (targetFocusRect.bottom > 1000)
        {
            int dif = targetFocusRect.bottom -1000;
            targetFocusRect.bottom = 1000;
            targetFocusRect.top -=dif;
        }
        return targetFocusRect;
    }
}
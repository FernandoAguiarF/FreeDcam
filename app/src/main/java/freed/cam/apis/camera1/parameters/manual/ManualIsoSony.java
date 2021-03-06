package freed.cam.apis.camera1.parameters.manual;

import android.hardware.Camera;

import freed.cam.apis.basecamera.CameraWrapperInterface;
import freed.cam.apis.basecamera.parameters.AbstractParameter;
import freed.cam.apis.camera1.parameters.ParametersHandler;
import freed.settings.SettingKeys;
import freed.settings.SettingsManager;

/**
 * Created by troop on 18.03.2017.
 */

public class ManualIsoSony extends AbstractParameter
{
    private final Camera.Parameters parameters;

    public ManualIsoSony(CameraWrapperInterface cameraUiWrapper, Camera.Parameters parameters, SettingKeys.Key key) {
        super(cameraUiWrapper,key);
        this.parameters = parameters;
        setViewState(ViewState.Visible);
    }

    @Override
    public void setValue(int valueToSet, boolean setToCamera)
    {
        currentInt = valueToSet;
        if (currentInt == 0)
        {
            if (cameraUiWrapper.getParameterHandler().get(SettingKeys.M_ExposureTime).GetValue() == 0)
                parameters.set("sony-ae-mode", "auto");
            else if (cameraUiWrapper.getParameterHandler().get(SettingKeys.M_ExposureTime).GetValue() >0)
                parameters.set("sony-ae-mode", "shutter-prio");
        }
        else {
            if (cameraUiWrapper.getParameterHandler().get(SettingKeys.M_ExposureTime).GetValue() == 0 && !parameters.get("sony-ae-mode").equals("iso-prio"))
                parameters.set("sony-ae-mode", "iso-prio");
            else if (cameraUiWrapper.getParameterHandler().get(SettingKeys.M_ExposureTime).GetValue() >0 && !parameters.get("sony-ae-mode").equals("manual"))
                parameters.set("sony-ae-mode", "manual");
            parameters.set(SettingsManager.get(SettingKeys.M_ManualIso).getKEY(), stringvalues[currentInt]);
        }
        ((ParametersHandler) cameraUiWrapper.getParameterHandler()).SetParametersToCamera(parameters);
    }

}

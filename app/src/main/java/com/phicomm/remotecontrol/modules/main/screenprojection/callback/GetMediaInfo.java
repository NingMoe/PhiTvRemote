package com.phicomm.remotecontrol.modules.main.screenprojection.callback;

import com.phicomm.remotecontrol.modules.main.screenprojection.entity.MediaInfo;
import com.phicomm.remotecontrol.util.LogUtil;

import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.InvalidValueException;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;

import java.util.Map;

/**
 * Created by kang.sun on 2017/8/22.
 */

public abstract class GetMediaInfo extends UpnpActionCallback {
    private static String TAG = GetMediaInfo.class.getSimpleName();

    public GetMediaInfo(UnsignedIntegerFourBytes instanceId, Service service) {
        super(new ActionInvocation(service.getAction("GetMediaInfo")));
        try {
            setInput("InstanceID", instanceId);
        } catch (InvalidValueException e) {
            LogUtil.e(TAG, e.toString());
        }
    }

    @Override
    public void received(ActionInvocation invocation, Map<String, Object> result) {
        MediaInfo mediaInfo = new MediaInfo(result);
        onSuccess(mediaInfo);
    }

    public abstract void onSuccess(MediaInfo mediaInfo);
}

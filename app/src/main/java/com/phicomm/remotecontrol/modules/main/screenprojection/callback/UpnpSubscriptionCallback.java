package com.phicomm.remotecontrol.modules.main.screenprojection.callback;

import com.phicomm.remotecontrol.util.LogUtil;

import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.state.StateVariableValue;
import org.fourthline.cling.support.lastchange.Event;
import org.fourthline.cling.support.lastchange.EventedValue;
import org.fourthline.cling.support.lastchange.InstanceID;
import org.fourthline.cling.support.lastchange.LastChangeParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kang.sun on 2017/8/23.
 */
public abstract class UpnpSubscriptionCallback extends SubscriptionCallback {
    private static String TAG = UpnpSubscriptionCallback.class.getSimpleName();

    public UpnpSubscriptionCallback(Service service) {
        super(service);
    }

    public UpnpSubscriptionCallback(Service service, int requestedDurationSeconds) {
        super(service, requestedDurationSeconds);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void eventReceived(GENASubscription subscription) {
        StateVariableValue svv = (StateVariableValue) subscription.getCurrentValues().get("LastChange");
        LogUtil.d(TAG, "eventReceived:" + svv.getValue().toString());
        LastChangeParser parser = getLastChangeParser();
        try {
            Event event = parser.parse(svv.getValue().toString());
            List<InstanceID> iIDs = event.getInstanceIDs();
            List<EventedValue> values = new ArrayList<EventedValue>();
            for (InstanceID iID : iIDs) {
                values.addAll(iID.getValues());
            }
            onReceive(values);
        } catch (Exception e) {
            LogUtil.e(TAG, e.toString());
        }
    }

    public abstract LastChangeParser getLastChangeParser();

    public abstract void onReceive(List<EventedValue> values);
}


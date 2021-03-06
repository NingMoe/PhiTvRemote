package com.phicomm.remotecontrol.util;

import android.util.Log;

import com.phicomm.remotecontrol.RemoteBoxDevice;
import com.phicomm.remotecontrol.greendao.Entity.RemoteDevice;
import com.phicomm.remotecontrol.greendao.GreenDaoUserUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by chunya02.li on 2017/7/7.
 */

public class DevicesUtil {

    private static String TAG = "DevicesUtil";

    public static List<RemoteDevice> mRecentedConnectedList = new ArrayList<>();

    private static Map<Long, String> mRecentedConnectedMap = new HashMap<>();

    public static List<RemoteBoxDevice> mCurrentDevicesListResult = new ArrayList<>();

    public static GreenDaoUserUtil mGreenDaoUserUtil;

    public static RemoteBoxDevice mTarget;


    public static boolean connectToEntry(String ipAddress) {
        return true;
    }

    public static List<RemoteBoxDevice> getCurrentDevicesListResult() {
        if (mCurrentDevicesListResult != null) {
            LogUtil.d(TAG, "get currentListResult.size=" + mCurrentDevicesListResult.size());
        }
        return mCurrentDevicesListResult;
    }

    public static void setCurrentListResult(List<RemoteBoxDevice> currentListResult) {
        Log.d(TAG, "set currentListResult.size=" + currentListResult.size());
        mCurrentDevicesListResult = currentListResult;
    }

    public static void insertOrUpdateRecentDevices(RemoteBoxDevice remoteDevice) {
        loadRecentList();
        RemoteDevice device = new RemoteDevice(null, remoteDevice.getName(), remoteDevice.getAddress(), remoteDevice.getPort(), remoteDevice.getBssid(), System.currentTimeMillis());
        Log.d(TAG, "insertOrUpdateRecentDevices remoteDevice=" + remoteDevice + "mRecentedConnectedMap.size()=" + mRecentedConnectedMap.size());
        Set<Long> ids = mRecentedConnectedMap.keySet();
        Long findId = null;
        for (Long id : ids) {
            String tempBssid = mRecentedConnectedMap.get(id);
            LogUtil.d(TAG, "tempBssid=" + tempBssid + " remoteDevice.getBssid()=" + remoteDevice.getBssid());
            if (tempBssid == null || remoteDevice == null) {
                continue;
            }
            if (tempBssid.equals(remoteDevice.getBssid())) {
                findId = id;
                break;
            }
        }
        Log.d(TAG, "findId=" + findId);
        if (findId != null) {
            device.setId(findId);
            mGreenDaoUserUtil.upateData(device);
        } else {
            mGreenDaoUserUtil.insertdata(device);
        }
        setTarget(remoteDevice);
    }

    public static void modifyDeviceInfo(RemoteBoxDevice remoteDevice) {
        loadRecentList();
        RemoteDevice device = null;
        for (int i = 0; i < mRecentedConnectedList.size(); i++) {
            RemoteDevice mDevice = mRecentedConnectedList.get(i);
            if (mDevice.getBssid().equals(remoteDevice.getBssid())) {
                device = new RemoteDevice(null, remoteDevice.getName(),
                        remoteDevice.getAddress(), remoteDevice.getPort(), remoteDevice.getBssid(), mDevice.getTime());
                break;
            }
        }

        Set<Long> ids = mRecentedConnectedMap.keySet();
        Long findId = null;
        for (Long id : ids) {
            String tempBssid = mRecentedConnectedMap.get(id);
            LogUtil.d(TAG, "tempBssid=" + tempBssid + " remoteDevice.getBssid()=" + remoteDevice.getBssid());
            if (tempBssid == null || remoteDevice == null) {
                continue;
            }
            if (tempBssid.equals(remoteDevice.getBssid())) {
                findId = id;
                break;
            }
        }

        if (findId != null) {
            device.setId(findId);
            mGreenDaoUserUtil.upateData(device);
        }
    }

    public static void loadRecentList() {
        mRecentedConnectedMap.clear();
        mRecentedConnectedList.clear();
        mRecentedConnectedList = mGreenDaoUserUtil.loadAllRecentByTimeOrder();
        for (int i = 0; i < mRecentedConnectedList.size(); i++) {
            mRecentedConnectedMap.put(mRecentedConnectedList.get(i).getId(), mRecentedConnectedList.get(i).getBssid());
        }
        Log.d(TAG, "mRecentedConnectedMap.size()=" + mRecentedConnectedMap);
    }

    public static void setGreenDaoUserUtil(GreenDaoUserUtil GreenDaoUserUtil) {
        mGreenDaoUserUtil = GreenDaoUserUtil;
    }

    public static void setTarget(RemoteBoxDevice target) {
        Log.d(TAG, "setTarget target=" + target);
        mTarget = target;
    }

    public static RemoteBoxDevice getTarget() {
        Log.d(TAG, "getTarget mTarget=" + mTarget);
        return mTarget;
    }
}


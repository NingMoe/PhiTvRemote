package com.phicomm.remotecontrol.modules.main.screenprojection.presenter;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.phicomm.remotecontrol.R;
import com.phicomm.remotecontrol.base.BaseApplication;
import com.phicomm.remotecontrol.modules.main.screenprojection.activities.LocalMediaItemActivity;
import com.phicomm.remotecontrol.modules.main.screenprojection.activities.LocalMediaItemView;
import com.phicomm.remotecontrol.modules.main.screenprojection.activities.PictureControlActivity;
import com.phicomm.remotecontrol.modules.main.screenprojection.activities.VideoControlActivity;
import com.phicomm.remotecontrol.modules.main.screenprojection.adapter.ContentAdapter;
import com.phicomm.remotecontrol.modules.main.screenprojection.adapter.GeneralAdapter;
import com.phicomm.remotecontrol.modules.main.screenprojection.constants.DeviceDisplayListOperation;
import com.phicomm.remotecontrol.modules.main.screenprojection.entity.ContentItem;
import com.phicomm.remotecontrol.modules.main.screenprojection.entity.DeviceDisplay;
import com.phicomm.remotecontrol.modules.main.screenprojection.entity.PictureContentItemList;
import com.phicomm.remotecontrol.modules.main.screenprojection.listener.TestRegistryListener;
import com.phicomm.remotecontrol.modules.main.screenprojection.model.ContentBrowseBiz;
import com.phicomm.remotecontrol.modules.main.screenprojection.model.MediaContentBiz;
import com.phicomm.remotecontrol.modules.main.screenprojection.model.UpnpServiceBiz;
import com.phicomm.remotecontrol.modules.main.screenprojection.server.MediaServer;
import com.phicomm.remotecontrol.modules.main.screenprojection.utils.FiletypeUtil;
import com.phicomm.remotecontrol.modules.main.screenprojection.utils.IPUtil;
import com.phicomm.remotecontrol.util.CommonUtils;
import com.phicomm.remotecontrol.util.DevicesUtil;
import com.phicomm.remotecontrol.util.LogUtil;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.support.model.item.Item;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by kang.sun on 2017/8/21.
 */
public class LocalMediaItemPresenterImpl implements LocalMediaItemPresenter {
    private static String TAG = "LocalMediaItemPresenterImpl";
    private Context mContext;
    private LocalMediaItemView mView;
    private BaseApplication mBaseApplication;
    private GeneralAdapter<ContentItem> mContentAdapter;
    private DeviceDisplay mDeviceDisplay;
    private ContentBrowseBiz mContentBrowseBiz;
    private Handler mHandler;
    private PictureContentItemList mPictureContentItemList;
    private TestRegistryListener rListener;
    private UpnpServiceBiz upnpServiceBiz;
    private MediaServer mediaServer;

    public LocalMediaItemPresenterImpl() {
    }

    public LocalMediaItemPresenterImpl(LocalMediaItemView mView, Context mContext, BaseApplication mBaseApplication) {
        this.mView = mView;
        this.mContext = mContext;
        this.mBaseApplication = mBaseApplication;
        getItems();
    }

    public void init(Context mContext, Handler mHandle) {
        upnpServiceBiz = UpnpServiceBiz.newInstance();
        rListener = new TestRegistryListener(mHandle);
        upnpServiceBiz.addListener(rListener);
        LogUtil.d(TAG, "开始init");
        // 初始化ContentDirectory服务
        try {
            // 启动ContentDirectory服务
            InetAddress localAddress = IPUtil.getLocalIpAddress(mContext);
            LogUtil.d(TAG, "开始mediaServer");
            if (mediaServer == null) {
                mediaServer = new MediaServer(localAddress);
                LogUtil.d(TAG, "已创建mediaServer");
                upnpServiceBiz.addDevice(mediaServer.getDevice());
                MediaContentBiz mediaContentBiz = new MediaContentBiz();
                mediaContentBiz.prepareMediaServer(mContext, mediaServer.getAddress());
            } else {
                LogUtil.d(TAG, "mediaServer已经存在" + mediaServer.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d(TAG, "Creating ContentDirectory device failed");
        }
    }

    private void getItems() {
        mPictureContentItemList = PictureContentItemList.getInstance();
        mContentAdapter = new ContentAdapter(mContext, R.layout.item_content, null);
        mContentAdapter.setMaxCount(1000);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case DeviceDisplayListOperation.ADD:
                        LogUtil.d(TAG, "开始添加");
                        ContentItem mContentItem = (ContentItem) msg.obj;
                        if (msg.arg2 == 1) {
                            if (!mPictureContentItemList.getPictureContentItemList().contains(mContentItem))
                                LogUtil.d(TAG, "添加的图片是： " + msg.obj.toString());
                            mPictureContentItemList.addPictureContentItem(mContentItem.getItem());
                        }
                        mContentAdapter.add(mContentItem);
                        break;
                    case DeviceDisplayListOperation.CLEAR_ALL:
                        mContentAdapter.clear();
                        break;
                }
            }
        };
        mDeviceDisplay = mBaseApplication.getDeviceDisplay();
        mBaseApplication.setDeviceDisplay(null);
        mContentBrowseBiz = new ContentBrowseBiz(mHandler);
        if (mDeviceDisplay == null) {
            LogUtil.d(TAG, "mDeviceDisplay == null");
        } else {
            LogUtil.d(TAG, "mDeviceDisplay是：" + mDeviceDisplay);
        }
        if (mDeviceDisplay.getDevice() == null) {
            LogUtil.d(TAG, "mDeviceDisplay.getDevice()==null");
        } else {
            LogUtil.d(TAG, "mDeviceDisplay.getDevice()是：" + mDeviceDisplay.getDevice());
            mContentBrowseBiz.getRootContent(mDeviceDisplay.getDevice());
        }
    }

    @Override
    public void showItems() {
        mView.showItems(mContentAdapter);
    }

    @Override
    public void browserItems(int position, LocalMediaItemActivity mLocalMediaItemActivity) {
        ContentItem mContentItem = mContentAdapter.getItem(position);
        browserSubContainer(mContentBrowseBiz, mContentItem, mLocalMediaItemActivity);
    }

    public void browserSubContainer(ContentBrowseBiz mContentBrowseBiz, ContentItem mContentItem, LocalMediaItemActivity mLocalMediaItemActivity) {
        if (mContentItem.isContainer()) {
            mContentBrowseBiz.getContent(mContentItem);
        } else {
            // 打开文件
            Item mItem = mContentItem.getItem();
            Intent intent = null;
            switch (mContentItem.getFiletype()) {
                case FiletypeUtil.FILETYPE_PIC:
                    selectDMPToPlay(mItem, mLocalMediaItemActivity, PictureControlActivity.class);
                    break;
                case FiletypeUtil.FILETYPE_MOVIE:
                    selectDMPToPlay(mItem, mLocalMediaItemActivity, VideoControlActivity.class);
                    break;
            }
        }
    }

    private void selectDMPToPlay(final Item mItem, LocalMediaItemActivity mLocalMediaItemActivity, Class<?> cls) {
        boolean flag = false;
        ArrayList<Device> mPlayDeviceList = new ArrayList<Device>(
                UpnpServiceBiz.newInstance().getDevices(new UDAServiceType("AVTransport")));
        for (int i = 0; i < mPlayDeviceList.size(); i++) {
            if (mPlayDeviceList.get(i).toString().indexOf(DevicesUtil.getTarget().getAddress()) != -1) {
                //if (mPlayDeviceList.get(i).toString().indexOf("172.20.10.9") != -1) {
                LogUtil.d(TAG, "已经找到对应设备，开始投屏");
                BaseApplication mBaseApplication = (BaseApplication) mLocalMediaItemActivity.getApplication();
                mBaseApplication.setDeviceDisplay(new DeviceDisplay(mPlayDeviceList.get(i)));
                mBaseApplication.setItem(mItem);
                CommonUtils.startIntent(mLocalMediaItemActivity, null, cls);
                flag = true;
            }
        }
        if (!flag) {
            mView.showMessage("设备已掉线，请重新连接");
        }
    }

    public void destory() {
        if (upnpServiceBiz != null) {
            upnpServiceBiz.removeListener(rListener);
            upnpServiceBiz = null;
        }
    }
}
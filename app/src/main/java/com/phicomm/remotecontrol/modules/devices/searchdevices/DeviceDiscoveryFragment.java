package com.phicomm.remotecontrol.modules.devices.searchdevices;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.phicomm.remotecontrol.ConnectManager;
import com.phicomm.remotecontrol.R;
import com.phicomm.remotecontrol.RemoteBoxDevice;
import com.phicomm.remotecontrol.activities.RecentDevicesActivity;
import com.phicomm.remotecontrol.constant.PhiConstants;
import com.phicomm.remotecontrol.modules.devices.searchdevices.DeviceDiscoveryContract.Presenter;
import com.phicomm.remotecontrol.util.DevicesUtil;
import com.phicomm.remotecontrol.util.LogUtil;

import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.Context.WIFI_SERVICE;


/**
 * Created by chunya02.li on 2017/7/11.
 */

public class DeviceDiscoveryFragment extends Fragment implements DeviceDiscoveryContract.View {
    private static String TAG = "DeviceDiscoveryFragment";
    @BindView(R.id.discovery_devices_list)
    public ListView mDiscoveryListDevices;
    @BindView(R.id.start_discovery)
    public Button mDiscoveryBtn;
    @BindView(R.id.manual_ip)
    public Button mManualIpBtn;
    @BindView(R.id.reccent_devices)
    public Button mRecentDevicesBtn;
    @BindView(R.id.local_networkname)
    public TextView mNetworkNameTv;
    @BindView(R.id.choose_text)
    public TextView mChooseTv;
    @BindView(R.id.device_count)
    public TextView mCountTv;
    @BindView(android.R.id.empty)
    public TextView mEmptyTv;
    @BindView(R.id.back)
    public ImageView mBackIv;
    @BindView(R.id.title)
    public TextView mTitleTv;
    @BindView(R.id.roundProgressBar)
    public RoundProgressBar mProgressBar;

    private Presenter mPresenter;
    private DeviceDiscoveryAdapter mDiscoveryAdapter;
    private DiscoveryHandler mBroadcastHandler;
    private WifiManager mWifiManager;
    private WifiChangeReceiver mWifiChangeReceiver;
    private ProgressBarTask mTask;
    private boolean mProgressBar_flag;

    public DeviceDiscoveryFragment() {

    }

    public static DeviceDiscoveryFragment newInstance() {
        AtomicReference<DeviceDiscoveryFragment> fragment = new AtomicReference<>(new
                DeviceDiscoveryFragment());
        return fragment.get();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_discovery_device, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        initAdapter();
        initActionBar();
        setOnClickListener();
        mBroadcastHandler = new DiscoveryHandler();


    }

    private void initActionBar() {
        Intent intent = getActivity().getIntent();
        Bundle bundle = intent.getExtras();
        String title = bundle.getString(PhiConstants.ACTION_BAR_NAME);
        mTitleTv.setText(title);
    }


    private void initAdapter() {
        mDiscoveryListDevices.setEmptyView(mEmptyTv);
        mDiscoveryListDevices.setDivider(new ColorDrawable(Color.GRAY));
        mDiscoveryListDevices.setDividerHeight(1);
        mDiscoveryAdapter = new DeviceDiscoveryAdapter();
        mDiscoveryListDevices.setAdapter(mDiscoveryAdapter);
    }

    private void setOnClickListener() {
        mDiscoveryListDevices.setOnItemClickListener(selectHandler);
        mDiscoveryBtn.setOnClickListener(onClickListener);
        mManualIpBtn.setOnClickListener(onClickListener);
        mRecentDevicesBtn.setOnClickListener(onClickListener);
        mBackIv.setOnClickListener(onClickListener);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mDiscoveryBtn) {
                if (isWifiAvailable()) {
                    startJmdnsDiscoveryDevice();
                } else {
                    Toast.makeText(getContext(), R.string.finder_wifi_not_available, Toast.LENGTH_SHORT).show();
                }

            } else if (v == mManualIpBtn) {
                buildManualIpDialog().show();
            } else if (v == mRecentDevicesBtn) {
                Intent intent = new Intent(getContext(), RecentDevicesActivity.class);
                startActivity(intent);
            } else if (v == mBackIv) {
                getActivity().onBackPressed();
            }
        }
    };

    private AdapterView.OnItemClickListener selectHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position,
                                long id) {
            final int pos = position;
            final RemoteBoxDevice remoteDevice = (RemoteBoxDevice) parent.getItemAtPosition(
                    position);
            if (remoteDevice != null) {
                ConnectManager.getInstance().connect(remoteDevice, new ConnectManager.ConnetResultCallback() {

                    @Override
                    public void onSuccess(RemoteBoxDevice device) {
                        RemoteBoxDevice target = mPresenter.getTarget();
                        if (target == null || !(target.getBssid().equals(remoteDevice.getBssid()))) {
                            mPresenter.insertOrUpdateRecentDevices(remoteDevice);
                            mDiscoveryAdapter.clearStates(pos);
                            mDiscoveryAdapter.notifyDataSetInvalidated();
                            mTitleTv.setText(remoteDevice.getName());

                            Toast.makeText(getContext(), "connect success", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFail(String msg) {
                        mPresenter.removeItemAndRefreshView(remoteDevice);
                        LogUtil.d(TAG, "remove connect fail device");
                        Toast.makeText(getContext(), "this device is offline", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };


    @Override
    public void onResume() {
        LogUtil.d(TAG, "onResume() is called");
        mPresenter.getCurrentDeviceList();
        LogUtil.d(TAG, mPresenter.getCurrentDeviceList().toString());
        mPresenter.loadRecentList();

        mWifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(WIFI_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiChangeReceiver = new WifiChangeReceiver();
        getActivity().registerReceiver(mWifiChangeReceiver, filter);

        super.onResume();
    }

    @Override
    public void onPause() {
        //按下返回键时强制终止ProgressBarTask
        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
            mProgressBar_flag = true;
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mWifiChangeReceiver);
        super.onStop();
    }

    @Override
    public void setPresenter(Presenter presenter) {
        mPresenter = presenter;
    }


    @Override
    public void showToast(String str) {
        Toast.makeText(getContext(), str, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setTittle(String str) {
        mTitleTv.setText(str);
    }

    @Override
    public void refreshListView(List<RemoteBoxDevice> currentlist) {
        if (currentlist.size() > 0) {
            mChooseTv.setVisibility(View.VISIBLE);
        } else {
            mChooseTv.setVisibility(View.INVISIBLE);
        }
        mCountTv.setText(makeDeviceCountLabel(currentlist.size()));
        int findIndex = findTargetPos(currentlist);
        if (findIndex != -1) {
            mDiscoveryAdapter.clearStates(findIndex);
        }
        mDiscoveryAdapter.notifyDataChange(currentlist);
    }


    private class DiscoveryHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "timeout it is need dismiss mDiscoveryDialog and begin TimeoutDialog");
            switch (msg.what) {
                case PhiConstants.BROADCAST_TIMEOUT:
                    stopProgressBar();
                    stopDiscoveryService();
                    break;
            }
        }

    }

    private void startJmdnsDiscoveryDevice() {
        mPresenter.start();
        mBroadcastHandler.removeMessages(PhiConstants.BROADCAST_TIMEOUT);
        mTask = new ProgressBarTask(this.getContext());
        mTask.execute();
        mBroadcastHandler.sendEmptyMessageDelayed(PhiConstants.BROADCAST_TIMEOUT,
                PhiConstants.DISCOVERY_TIMEOUT);
    }


    private void stopDiscoveryService() {
        mBroadcastHandler.removeMessages(PhiConstants.BROADCAST_TIMEOUT);
        RemoteBoxDevice target = mPresenter.getTarget();
        if (target == null) {
            mTitleTv.setText(R.string.unable_to_connect_device);
        } else {
            mTitleTv.setText(target.getName());
        }
        mPresenter.stop();
    }


    public String makeDeviceCountLabel(int count) {
        StringBuilder deviceCount = new StringBuilder();
        StringBuilder formatBuilder = new StringBuilder();
        Formatter sFormatter = new Formatter(formatBuilder, Locale.getDefault());
        if (isAdded()) {
            String f = getResources().getQuantityText(R.plurals.NdeviceCount, count)
                    .toString();
            deviceCount.setLength(0);
            sFormatter.format(f, count);
            deviceCount.append(formatBuilder);
            return deviceCount.toString();
        } else {
            return String.valueOf(count);
        }
    }


    private AlertDialog buildManualIpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manual_ip, null);
        final EditText ipEditText =
                (EditText) view.findViewById(R.id.manual_ip_entry);
        ipEditText.setFilters(new InputFilter[]{
                new NumberKeyListener() {
                    @Override
                    protected char[] getAcceptedChars() {
                        return "0123456789.:".toCharArray();
                    }

                    public int getInputType() {
                        return InputType.TYPE_CLASS_NUMBER;
                    }
                }
        });

        builder.setPositiveButton(
                R.string.manual_ip_connect, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String inputStr = ipEditText.getText().toString().trim();
                        if (isValidIpAddress(inputStr)) {
                            mPresenter.ipConnect(inputStr);
                        } else {
                            Toast.makeText(getContext(),
                                    getString(R.string.manual_ip_error_address),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(
                        R.string.manual_ip_cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                .setCancelable(true)
                .setTitle(R.string.manual_ip_label)
                .setMessage(R.string.manual_ip_entry_label)
                .setView(view);
        return builder.create();
    }

    private boolean isValidIpAddress(String inputStr) {
        if (inputStr == null || inputStr.isEmpty()) {
            return false;
        }
        String ips[] = inputStr.split("\\.");
        if (ips.length == 4) {
            try {
                for (String ip : ips) {
                    if (Integer.parseInt(ip) < 0 || Integer.parseInt(ip) > 255) {
                        return false;
                    }
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private int findTargetPos(List<RemoteBoxDevice> currentList) {
        RemoteBoxDevice target = DevicesUtil.getTarget();
        if (target == null || !(currentList.size() > 0)) {
            return -1;
        }
        for (int i = 0; i < currentList.size(); i++) {
            if (target.getBssid().equals(currentList.get(i).getBssid())) {
                return i;
            }
        }
        return -1;
    }

    private void stopProgressBar() {
        mProgressBar.setVisibility(View.GONE);
        mDiscoveryBtn.setVisibility(View.VISIBLE);
        mManualIpBtn.setVisibility(View.VISIBLE);
        mDiscoveryBtn.setEnabled(true);
        mManualIpBtn.setEnabled(true);
        mRecentDevicesBtn.setEnabled(true);
        mDiscoveryListDevices.setEnabled(true);
    }

    private void startProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mDiscoveryBtn.setVisibility(View.GONE);
        mManualIpBtn.setVisibility(View.GONE);
        mDiscoveryBtn.setEnabled(false);
        mManualIpBtn.setEnabled(false);
        mRecentDevicesBtn.setEnabled(false);
        mDiscoveryListDevices.setEnabled(false);
    }

    class ProgressBarTask extends AsyncTask<Void, Integer, Void> {
        Context mContext;
        int mCurrentProgress = 0;

        public ProgressBarTask(Context ctx) {
            mContext = ctx;
        }

        @Override
        protected void onPreExecute() {
            startProgressBar();
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (mCurrentProgress < 100) {
                if (mProgressBar_flag || !isWifiAvailable()) {
                    mBroadcastHandler.sendEmptyMessage(PhiConstants.BROADCAST_TIMEOUT);
                    break;
                }
                mCurrentProgress += new Random().nextInt(15);
                publishProgress(mCurrentProgress);
                try {
                    Thread.sleep(600);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mProgressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Void result) {
            stopProgressBar();
        }


        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

    }

    private boolean isWifiAvailable() {
        if (!mWifiManager.isWifiEnabled()) {
            return false;
        }
        WifiInfo info = mWifiManager.getConnectionInfo();
        return info != null && info.getIpAddress() != 0;
    }

    private String getNetworkName() {
        if (!isWifiAvailable()) {
            return null;
        }
        WifiInfo info = mWifiManager.getConnectionInfo();
        String ssid = null;
        if (info != null && isWifiAvailable()) {
            int len = info.getSSID().length();
            if (info.getSSID().startsWith("\"")
                    && info.getSSID().endsWith("\"")) {
                ssid = info.getSSID().substring(1, len - 1);
            } else {
                ssid = info.getSSID();
            }
        }
        return ssid;
    }

    class WifiChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLING: {
                        mNetworkNameTv.setText(R.string.wifi_connecting);
                        break;
                    }
                    case WifiManager.WIFI_STATE_DISABLING: {
                        mNetworkNameTv.setText(R.string.wifi_disconnecting);
                        break;
                    }
                    case WifiManager.WIFI_STATE_DISABLED: {
                        mNetworkNameTv.setText(R.string.wifi_not_connected);
                        break;
                    }
                    default:
                        break;
                }
            }

            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null != parcelableExtra) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    NetworkInfo.State state = networkInfo.getState();
                    if (state == NetworkInfo.State.CONNECTED) {
                        mNetworkNameTv.setText(getNetworkName());
                    }
                }
            }

        }
    }

}



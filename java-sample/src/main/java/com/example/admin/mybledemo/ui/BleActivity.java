package com.example.admin.mybledemo.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.admin.mybledemo.BleRssiDevice;
import com.example.admin.mybledemo.Constant;
import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.adapter.ScanAdapter;
import com.google.gson.Gson;
import com.nexenio.bleindoorpositioning.location.distance.BeaconDistanceCalculator;
import com.pgyersdk.update.PgyUpdateManager;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleStatusCallback;
import cn.com.heaton.blelibrary.ble.model.ScanRecord;
import cn.com.heaton.blelibrary.ble.utils.Utils;
import cn.com.superLei.aoparms.annotation.Permission;
import cn.com.superLei.aoparms.annotation.PermissionDenied;
import cn.com.superLei.aoparms.annotation.PermissionNoAskDenied;
import cn.com.superLei.aoparms.common.permission.AopPermissionUtils;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class BleActivity extends AppCompatActivity {
    private String TAG = BleActivity.class.getSimpleName();
    public static final int REQUEST_PERMISSION_LOCATION = 2;
    public static final int REQUEST_PERMISSION_WRITE = 3;
    public static final int REQUEST_GPS = 4;
    private LinearLayout llBlutoothAdapterTip;
    private TextView tvAdapterStates;
    private SwipeRefreshLayout swipeLayout;
    private FloatingActionButton floatingActionButton;
    private RecyclerView recyclerView;
    private FilterView filterView;
    private ScanAdapter adapter;
    private List<BleRssiDevice> bleRssiDevices;
    private Ble<BleRssiDevice> ble = Ble.getInstance();
    private ObjectAnimator animator;
    private AndSubView numAngel;
    private AndSubView numDistance;

    String clientId = "AndroidBLERSSIClient";
    private MqttAndroidClient mqttAndroidClient;
    final String serverUri = "tcp://iot.dcideal.com:1883";
    String _phoneId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        initView();
        initAdapter();
        initLinsenter();
        initBleStatus();
        requestPermission();

        initMQTTClient();

        genDeviceId();
    }

    private void genDeviceId() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //获取保存在sd中的 设备唯一标识符
                    String readDeviceID = com.example.admin.mybledemo.Utils.GetDeviceId.readDeviceID(BleActivity.this);
                    //获取缓存在  sharepreference 里面的 设备唯一标识
                    String string = com.example.admin.mybledemo.Utils.SPUtils.get(BleActivity.this, Constant.Constance.SP_DEVICES_ID, readDeviceID);
//                    String string = com.example.admin.mybledemo.Utils.
                    //判断 app 内部是否已经缓存,  若已经缓存则使用app 缓存的 设备id
                    if (string != null) {
                        //app 缓存的和SD卡中保存的不相同 以app 保存的为准, 同时更新SD卡中保存的 唯一标识符
                        if (StringUtils.isBlank(readDeviceID) && !string.equals(readDeviceID)) {
                            // 取有效地 app缓存 进行更新操作
                            if (StringUtils.isBlank(readDeviceID) && !StringUtils.isBlank(string)) {
                                readDeviceID = string;
                                com.example.admin.mybledemo.Utils.GetDeviceId.saveDeviceID(readDeviceID, BleActivity.this);
                            }
                        }
                    }
                    // app 没有缓存 (这种情况只会发生在第一次启动的时候)
                    if (StringUtils.isBlank(readDeviceID)) {
                        //保存设备id
                        readDeviceID = com.example.admin.mybledemo.Utils.GetDeviceId.getDeviceId(BleActivity.this);
                    }
                    //左后再次更新app 的缓存
                    com.example.admin.mybledemo.Utils.SPUtils.put(BleActivity.this, Constant.Constance.SP_DEVICES_ID, readDeviceID);
                    _phoneId = readDeviceID;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initMQTTClient() {
        clientId = clientId + System.currentTimeMillis();
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
//                    addToHistory("Reconnected to : " + serverURI);
//                    // Because Clean Session is true, we need to re-subscribe
//                    subscribeToTopic();
                } else {
//                    addToHistory("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
//                addToHistory("The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
//                addToHistory("Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);

        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
//                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    addToHistory("Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    @Permission(value = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
            requestCode = REQUEST_PERMISSION_WRITE,
            rationale = "读写SD卡权限被拒绝,将会影响自动更新版本功能哦!")
    private void update() {
        new PgyUpdateManager.Builder()
                .setForced(false)                //设置是否强制更新
                .setUserCanRetry(false)         //失败后是否提示重新下载
                .setDeleteHistroyApk(true)     // 检查更新前是否删除本地历史 Apk
                .register();
    }

    private void initAdapter() {
        bleRssiDevices = new ArrayList<>();
        adapter = new ScanAdapter(this, bleRssiDevices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        recyclerView.getItemAnimator().setChangeDuration(300);
        recyclerView.getItemAnimator().setMoveDuration(300);
        recyclerView.setAdapter(adapter);
    }

    private void initView() {
        llBlutoothAdapterTip = findViewById(R.id.ll_adapter_tip);
        swipeLayout = findViewById(R.id.swipeLayout);
        tvAdapterStates = findViewById(R.id.tv_adapter_states);
        recyclerView = findViewById(R.id.recyclerView);
        floatingActionButton = findViewById(R.id.floatingButton);
        filterView = findViewById(R.id.filterView);
        numAngel = findViewById(R.id.numAngel);
        numDistance = findViewById(R.id.numDistance);
        filterView.init(this);
    }

    private void initLinsenter() {
        tvAdapterStates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, Ble.REQUEST_ENABLE_BT);
            }
        });
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rescan();
            }
        });
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeLayout.setRefreshing(false);
                rescan();
            }
        });

    }

    //请求权限
    @Permission(value = {Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},
            requestCode = REQUEST_PERMISSION_LOCATION,
            rationale = "需要蓝牙相关权限")
    public void requestPermission() {
        checkBlueStatus();
        update();
    }

    @PermissionDenied
    public void permissionDenied(int requestCode, List<String> denyList) {
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            Log.e(TAG, "permissionDenied>>>:定位权限被拒 " + denyList.toString());
        } else if (requestCode == REQUEST_PERMISSION_WRITE) {
            Log.e(TAG, "permissionDenied>>>:读写权限被拒 " + denyList.toString());
        }
    }

    @PermissionNoAskDenied
    public void permissionNoAskDenied(int requestCode, List<String> denyNoAskList) {
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            Log.e(TAG, "permissionNoAskDenied 定位权限被拒>>>: " + denyNoAskList.toString());
        } else if (requestCode == REQUEST_PERMISSION_WRITE) {
            Log.e(TAG, "permissionDenied>>>:读写权限被拒>>> " + denyNoAskList.toString());
        }
        AopPermissionUtils.showGoSetting(this, "为了更好的体验，建议前往设置页面打开权限");
    }

    //监听蓝牙开关状态
    private void initBleStatus() {
        ble.setBleStatusCallback(new BleStatusCallback() {
            @Override
            public void onBluetoothStatusChanged(boolean isOn) {
                BleLog.i(TAG, "onBluetoothStatusOn: 蓝牙是否打开>>>>:" + isOn);
                llBlutoothAdapterTip.setVisibility(isOn?View.GONE:View.VISIBLE);
                if (isOn){
                    checkGpsStatus();
                }else {
                    if (ble.isScanning()) {
                        ble.stopScan();
                    }
                }
            }
        });
    }

    //检查蓝牙是否支持及打开
    private void checkBlueStatus() {
        if (!ble.isSupportBle(this)) {
            com.example.admin.mybledemo.Utils.showToast(R.string.ble_not_supported);
            finish();
        }
        if (!ble.isBleEnable()) {
            llBlutoothAdapterTip.setVisibility(View.VISIBLE);
        }else {
            checkGpsStatus();
        }
    }

    private void checkGpsStatus(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Utils.isGpsOpen(BleActivity.this)){
            new AlertDialog.Builder(BleActivity.this)
                    .setTitle("提示")
                    .setMessage("为了更精确的扫描到Bluetooth LE设备,请打开GPS定位")
                    .setPositiveButton("确定", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent,REQUEST_GPS);
                    })
                    .setNegativeButton("取消", null)
                    .create()
                    .show();
        }else {
            ble.startScan(scanCallback);
        }
    }

    private void rescan() {
        if (ble != null && !ble.isScanning()) {
            bleRssiDevices.clear();
            adapter.notifyDataSetChanged();
            ble.startScan(scanCallback);
        }



    }

    private BleScanCallback<BleRssiDevice> scanCallback = new BleScanCallback<BleRssiDevice>() {
        @Override
        public void onLeScan(final BleRssiDevice device, int rssi, byte[] scanRecord) {
            if (TextUtils.isEmpty(device.getBleName())) return;
            synchronized (ble.getLocker()) {
                for (int i = 0; i < bleRssiDevices.size(); i++) {
                    BleRssiDevice rssiDevice = bleRssiDevices.get(i);
                    if (TextUtils.equals(rssiDevice.getBleAddress(), device.getBleAddress())){
                        if (rssiDevice.getRssi() != rssi && System.currentTimeMillis()-rssiDevice.getRssiUpdateTime() >1000L){
                            rssiDevice.setRssiUpdateTime(System.currentTimeMillis());
                            rssiDevice.setRssi(rssi);
                            rssiDevice.setDistance(BeaconDistanceCalculator.calculateDistance(rssi, -67, 2.f));
                            pushNotify(device);
                            adapter.notifyItemChanged(i);
                        }
                        return;
                    }
                }
                device.setScanRecord(ScanRecord.parseFromBytes(scanRecord));
                device.setRssi(rssi);
                device.setDistance(BeaconDistanceCalculator.calculateDistance(rssi, -67, 2.f));
                bleRssiDevices.add(device);
                pushNotify(device);

                adapter.notifyDataSetChanged();
            }
        }

        private void pushNotify(final BleRssiDevice device) {
            // 本机机型以及唯一标识
            HashMap phone = new HashMap();
            phone.put("MANUFACTURER", Build.MANUFACTURER);
            phone.put("BRAND", Build.BRAND);
            phone.put("DEVICE", Build.DEVICE);
            phone.put("MODEL", Build.MODEL);
            phone.put("ID", _phoneId);
            phone.put("ANDROID_OS_RELEASE", Build.VERSION.RELEASE);

            // 标定的角度和位置
            HashMap calibrate_param = new HashMap();
            calibrate_param.put("angel", numAngel.getValue());
            calibrate_param.put("distance", numDistance.getValue());

            try {
                Gson gs = new Gson();
                MqttMessage message = new MqttMessage();
                HashMap payload = new HashMap();
                payload.put("ble", device);
                payload.put("phone", phone);
                payload.put("calibrate", calibrate_param);
                message.setPayload(gs.toJson(payload).getBytes());
                mqttAndroidClient.publish("BLE/RSSI", message);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            startBannerLoadingAnim();
        }

        @Override
        public void onStop() {
            super.onStop();
            stopBannerLoadingAnim();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed: "+errorCode);
        }
    };

    public void startBannerLoadingAnim() {
        floatingActionButton.setImageResource(R.drawable.ic_loading);
        animator = ObjectAnimator.ofFloat(floatingActionButton, "rotation", 0, 360);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(800);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
    }

    public void stopBannerLoadingAnim() {
        floatingActionButton.setImageResource(R.drawable.ic_bluetooth_audio_black_24dp);
        animator.cancel();
        floatingActionButton.setRotation(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_introduced:
                startActivity(new Intent(BleActivity.this, IntroducedActivity.class));
                break;
            case R.id.menu_share:
                com.example.admin.mybledemo.Utils.shareAPK(this);
                break;
            case R.id.menu_contribute:
                ImageView imageView = new ImageView(this);
                imageView.setImageResource(R.drawable.wechat);
                new AlertDialog.Builder(BleActivity.this)
                        .setTitle("打赏/联系作者")
                        .setView(imageView)
                        .create()
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == Ble.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
        } else if (requestCode == Ble.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            ble.startScan(scanCallback);
        }else if (requestCode == REQUEST_GPS){

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}

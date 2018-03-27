package com.example.admin.mybledemo.activity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admin.mybledemo.LeDeviceListAdapter;
import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.annotation.LLAnnotation;
import com.example.admin.mybledemo.annotation.ViewInit;
import com.example.admin.mybledemo.command.Command;
import com.orhanobut.logger.Logger;

import java.util.Arrays;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.callback.BleConnCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;

public class TestActivity extends AppCompatActivity {

    private static final String TAG = "TestActivity";
    @ViewInit(R.id.lv_scan)
    private ListView mListView;
    @ViewInit(R.id.notify_statue)
    private TextView mNotifyStatus;
    @ViewInit(R.id.notify_value)
    private TextView mNotifyValue;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Ble<BleDevice> mBle;
    private BleDevice mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        //初始化注解  替代findViewById
        LLAnnotation.viewInit(this);
        mDevice = (BleDevice) getIntent().getSerializableExtra("device");

        mBle = Ble.getInstance();

        initView();

    }

    //播放音乐
    public byte[] changeLevelInner(int play) {
        byte[] data = new byte[Command.qppDataSend.length];
        System.arraycopy(Command.qppDataSend, 0, data, 0, data.length);
        data[6] = 0x03;
        data[7] = (byte) play;
        Logger.e("data:" + Arrays.toString(data));
        return data;
    }

    //测试通知
    public void testNotify(View view) {
        if(mDevice != null){
            mNotifyStatus.setText("设置通知监听成功！！！");
            mBle.startNotify(mDevice, new BleNotiftCallback<BleDevice>() {
                @Override
                public void onChanged(BluetoothGattCharacteristic characteristic) {
                    Log.e(TAG, "onChanged: " + Arrays.toString(characteristic.getValue()));
                    mNotifyValue.setText("收到MCU通知值:\n"+Arrays.toString(characteristic.getValue()));
//                    Toast.makeText(TestActivity.this, Arrays.toString(characteristic.getValue()),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    //测试扫描
    public void testScan(View view){
        if (mBle != null && !mBle.isScanning()) {
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.addDevices(mBle.getConnetedDevices());
            mBle.startScan(new BleScanCallback<BleDevice>() {
                @Override
                public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
                    synchronized (mBle.getLocker()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mLeDeviceListAdapter.addDevice(device);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            });
        }
    }

    //测试发送
    public void testSend(View view){
        if(mDevice != null){
            //发送数据
            boolean result = mBle.write(mDevice, changeLevelInner(1), new BleWriteCallback<BleDevice>() {
                @Override
                public void onWriteSuccess(BluetoothGattCharacteristic characteristic) {
                    Toast.makeText(TestActivity.this, "发送数据成功", Toast.LENGTH_SHORT).show();
                }
            });
            if (!result) {
                Log.e(TAG, "changeLevelInner: " + "发送数据失败!");
            }
        }
    }

    //测试读取rssi值
    public void testRssi(View view){
        if(mDevice != null) {
            mBle.readRssi(mDevice, new BleReadRssiCallback<BleDevice>() {
                @Override
                public void onReadRssiSuccess(int rssi) {
                    super.onReadRssiSuccess(rssi);
                    Log.e(TAG, "onReadRssiSuccess: " + rssi);
                    Toast.makeText(TestActivity.this, "onReadRssiSuccess:" + rssi, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initView() {
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mLeDeviceListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //测试连接或断开
                final BleDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                if (mBle.isScanning()) {
                    mBle.stopScan();
                }
                if (device.isConnected()) {
                    mBle.disconnect(device, connectCallback);
                } else if (!device.isConnectting()) {
                    mBle.connect(device, connectCallback);
                }
            }
        });

    }

    /*连接的回调*/
    private BleConnCallback<BleDevice> connectCallback = new BleConnCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {
            if (device.isConnected()) {
                setNotify(device);
            }
            Log.e(TAG, "onConnectionChanged: " + device.isConnected());
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onConnectException(BleDevice device, int errorCode) {
            super.onConnectException(device, errorCode);
            Toast.makeText(TestActivity.this, "连接异常，异常状态码:" + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    /*设置通知的回调*/
    private void setNotify(BleDevice device) {
         /*连接成功后，设置通知*/
        mBle.startNotify(device, new BleNotiftCallback<BleDevice>() {
            @Override
            public void onChanged(BluetoothGattCharacteristic characteristic) {
                UUID uuid = characteristic.getUuid();
                Log.e(TAG, "onChanged: "+uuid.toString());
                Log.e(TAG, "onChanged: " + Arrays.toString(characteristic.getValue()));
            }

            @Override
            public void onReady(BleDevice device) {
                Log.e(TAG, "onReady: ");
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt) {
                Log.e(TAG, "onServicesDiscovered is success ");
            }

            @Override
            public void onNotifySuccess(BluetoothGatt gatt) {
                Log.e(TAG, "onNotifySuccess is success ");
            }
        });
    }
}

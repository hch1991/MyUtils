package com.hch.myutils.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import com.hch.myutils.interfaces.ScanBluetoothDeviceInterface;

import java.lang.reflect.Method;
import java.util.Set;

import static android.content.ContentValues.TAG;

/**
 * @author hechuang
 * @ClassName: BlueToothUtil.java
 * @Description: TODO
 * @date 2018/12/24 14:25
 */
public class BlueToothUtil {

    private BluetoothAdapter mBluetoothAdapter;
    private BlueBroadcastReceiver mBroadcastReceiver;
    private String HID_NAME = "";  // 连接的蓝牙设备名
    private String HID_ADDR = "";  //连接的蓝牙设备地址
    private HidInputUtil mHidInputUtil;
    private HidVideoUtil mHidVideoUtil;
    private Context mContext;
    private ScanBluetoothDeviceInterface mScanBluetoothDeviceInterface;
    private BluetoothDevice mConnectDevice;

    public BlueToothUtil(Context context) {
        mContext = context;
        // 4.0以上才支持HID模式
        if (Build.VERSION.SDK_INT >= 17) {
            mHidInputUtil = HidInputUtil.getInstance(mContext);
            mHidVideoUtil = HidVideoUtil.getInstance(mContext);
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * @param :
     * @return :
     * created at 2018/12/24 14:27
     * @Description: TODO 检查是否支持蓝牙
     * @author : hechuang
     */
    public Boolean checkHasBluetooth() {
        if (mBluetoothAdapter == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @param :
     * @return :
     * created at 2019/1/3 15:55
     * @Description: TODO 检查蓝牙是否打开
     * @author : hechuang
     */

    public Boolean checkBlueToothEnable() {
        if (mBluetoothAdapter.isEnabled()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param :
     * @return :
     * created at 2019/1/3 15:32
     * @Description: TODO 返回已绑定蓝牙列表
     * @author : hechuang
     */

    public Set<BluetoothDevice> getBondedDevices() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return mBluetoothAdapter.getBondedDevices();
    }

    /**
     * @param :
     * @return :
     * created at 2019/1/3 16:37
     * @Description: TODO 开始扫描蓝牙
     * @author : hechuang
     */

    public void startScanBlueDevice(ScanBluetoothDeviceInterface scanBluetoothDeviceInterface) {
        mScanBluetoothDeviceInterface = scanBluetoothDeviceInterface;
        mBroadcastReceiver = new BlueBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        // 先判断蓝牙是否在开启扫描中 如果没有开启则开启扫描  开启了则不管
        if (!mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.startDiscovery();
            mScanBluetoothDeviceInterface.scanStatus(true);
        }
    }

    /**
     * @param :
     * @return :
     * created at 2019/1/3 16:22
     * @Description: TODO 连接指令蓝牙
     * @author : hechuang
     */

    public void connectBlueTooth(BluetoothDevice bluetoothDevice) {
        mConnectDevice = bluetoothDevice;
        HID_NAME = mConnectDevice.getName();
        HID_ADDR = mConnectDevice.getAddress();
        mBluetoothAdapter.cancelDiscovery();
        mScanBluetoothDeviceInterface.scanStatus(false);
        if (mConnectDevice == null) {
            return;
        }
        int typeNum = mConnectDevice.getBluetoothClass().getMajorDeviceClass();
        switch (typeNum) {
            case 1024:
                if (!mHidVideoUtil.isBonded(mConnectDevice)) {
                    mHidVideoUtil.pair(mConnectDevice);
                } else {
                    mHidVideoUtil.connect(mConnectDevice);
                }
                break;
            case 1280:
                if (!mHidInputUtil.isBonded(mConnectDevice)) {
                    mHidInputUtil.pair(mConnectDevice);
                } else {
                    mHidInputUtil.connect(mConnectDevice);
                }
                break;
        }
    }

    private class BlueBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive:" + action);
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                // 通过广播接收到了BluetoothDevice
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
//                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // 防止重复添加
                    mScanBluetoothDeviceInterface.scanBluetoothDevice(device);
//                    if (temp1ScanBlueDeviceObjs.indexOf(device) == -1) {
//                        MLog.d("扫描到蓝牙：" + device.getName());
//                        temp1ScanBlueDeviceObjs.add(device);
//                        getScanBlueDeviceObjs(temp1ScanBlueDeviceObjs);
//                    }
                }
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                String address = device.getAddress();
                Log.i(TAG, "name:" + name + ",address:" + address + ",bondstate:" + device.getBondState());
                if ((address != null && address.equals(HID_ADDR)) || (name != null && name.equals(HID_NAME))) {
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        int typeNum = mConnectDevice.getBluetoothClass().getMajorDeviceClass();
                        switch (typeNum) {
                            case 1024:
                                mHidVideoUtil.connect(device);
                                break;
                            case 1280:
                                mHidInputUtil.connect(device);
                                break;
                        }
                    }
                }
            } else if (action.equals("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED")) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "state=" + state + ",device=" + device);
//                if(state == BluetoothProfile.STATE_CONNECTED){
//                    Toast.makeText(MainActivity.this, R.string.connnect_success, Toast.LENGTH_LONG).show();
//                } else if(state == BluetoothProfile.STATE_DISCONNECTED){
//                    Toast.makeText(MainActivity.this, R.string.disconnected, Toast.LENGTH_LONG).show();
//                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                MLog.d("扫描完毕");
                mScanBluetoothDeviceInterface.scanStatus(false);
//                startDiscovery();
            }
        }
    }

    /**
     * @Description: TODO 关闭设置蓝牙可见时间限制
     * @author : hechuang
     * @param :
     * @return :
     * created at 2019/1/3 16:55
     */
    public void closeDiscoverableTimeout() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, 1);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package com.ritu.upgrade;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.ritu.upgrade.fragment.UpgradeFragment;
import com.ritu.upgrade.tools.ActivityUtils;
import com.ritu.upgrade.tools.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author ritu on 14-Jun-18
 * */
public class MainActivity extends AppCompatActivity {

    private final String TAG = "升级工具";

    private static final String ACTION_USB_PERMISSION = "com.androidinspain.otgviewer.USB_PERMISSION";

    private UpgradeFragment mLauncherFragment;
    private PendingIntent mPermissionIntent;
    private UsbManager mUsbManager;


    private List<UsbDevice> mDetectedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLauncherFragment = new UpgradeFragment();
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mDetectedDevices = new ArrayList<>();
        ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                mLauncherFragment, R.id.contentFrame);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        checkUSBStatus();
    }

    private void checkUSBStatus() {
        Log.d(TAG, "checkUSBStatus");
        mDetectedDevices.clear();
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (mUsbManager != null) {
            HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
            if (!deviceList.isEmpty()) {
                for (UsbDevice device : deviceList.values()) {
                    if (FileUtils.isMassStorageDevice(device)) {
                        mDetectedDevices.add(device);
                    }
                }
            }
        }
    }

    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            checkUSBStatus();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
//                removedUSB();
            }
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
//                            connectDevice(device);
                        }
                    } else {
                        Log.e(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };




}

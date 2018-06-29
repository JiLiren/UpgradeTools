package com.ritu.upgrade;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.partition.Partition;
import com.ritu.upgrade.event.IVIew;
import com.ritu.upgrade.fragment.UpgradeFragment;
import com.ritu.upgrade.tools.ActivityUtils;
import com.ritu.upgrade.tools.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author ritu on 14-Jun-18
 * */
public class MainActivity extends AppCompatActivity implements IVIew {

    private final String TAG = "升级工具";

    private static final String ACTION_USB_PERMISSION = "com.androidinspain.otgviewer.USB_PERMISSION";

    private UpgradeFragment mLauncherFragment;
    private PendingIntent mPermissionIntent;
    private UsbManager mUsbManager;

    private UsbDevice usbDevice;

    private List<UsbDevice> mDetectedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLauncherFragment = new UpgradeFragment();
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mDetectedDevices = new ArrayList<>();


        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        mWakeLock.acquire();
        mWakeLock.release();
        ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                mLauncherFragment, R.id.contentFrame);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        iFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        iFilter.addDataScheme("file");
        registerReceiver(mUsbReceiver, iFilter);
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
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                if (device != null) {
                    mUsbManager.hasPermission(device);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
    }

    @Override
    public UsbDevice getUsbDevice() {
        return usbDevice;
    }


}
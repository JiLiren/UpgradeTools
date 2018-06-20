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
import android.widget.Toast;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.partition.Partition;
import com.ritu.upgrade.event.IVIew;
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
public class MainActivity extends AppCompatActivity implements IVIew{

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
        iFilter.addDataScheme("file"); registerReceiver(mUsbReceiver, iFilter);
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
            switch (action) {
                //接受到自定义广播
                case ACTION_USB_PERMISSION:
                    synchronized (this) {
                        UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        //允许权限申请
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (usbDevice != null) {
                                MainActivity.this.usbDevice = usbDevice;
                                //Do something\


                            }
                        } else {
                            Toast.makeText(context,"没有接受权限申请，无法工作",Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                //接收到存储设备插入广播
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    UsbDevice device_add = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device_add != null) {
//                        TShow("接收到存储设备插入广播");
                    }
                    break;
                //接收到存储设备拔出广播
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    UsbDevice device_remove = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device_remove != null) {
                        Toast.makeText(context,"USB设备被拔出",Toast.LENGTH_SHORT).show();
                        //拔出或者碎片 Activity销毁时 释放引用
                        //device.close();
                    }
                    break;
                default:break;
            }

//            String action = intent.getAction();
//            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
//                //USB设备移除，更新UI
//            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
//                //USB设备挂载，更新UI
//                // String usbPath = intent.getDataString();（usb在手机上的路径）
//            }
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

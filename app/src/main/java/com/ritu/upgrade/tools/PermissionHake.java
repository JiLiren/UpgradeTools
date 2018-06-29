package com.ritu.upgrade.tools;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class PermissionHake {

    public static void on(Activity activity){
        Intent intent = activity.getIntent();
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        PendingIntent pendingIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
        String packageName = intent.getStringExtra("package");

        PackageManager packageManager = activity.getPackageManager();
        ApplicationInfo aInfo;
        try {
            aInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "unable to look up package name", e);
            return;
        }
        String appName = aInfo.loadLabel(packageManager).toString();
    }
}

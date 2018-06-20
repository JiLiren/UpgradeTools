package com.ritu.upgrade.tools;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ritu on 15-Jun-18
 * */
public class FileUtils {

    private static final String TAG = " FileUtils";

    /**
     * 是大容量存储设备
     * */
    public static boolean isMassStorageDevice(UsbDevice device) {
        boolean result = false;

        int interfaceCount = device.getInterfaceCount();
        for (int i = 0; i < interfaceCount; i++) {
            UsbInterface usbInterface = device.getInterface(i);
            Log.i(TAG, "found usb interface: " + usbInterface);

            // we currently only support SCSI transparent command set with
            // bulk transfers only!
            if (usbInterface.getInterfaceClass() != UsbConstants.USB_CLASS_MASS_STORAGE
                    || usbInterface.getInterfaceSubclass() != Constants.INTERFACE_SUBCLASS
                    || usbInterface.getInterfaceProtocol() != Constants.INTERFACE_PROTOCOL) {
                Log.i(TAG, "device interface not suitable!");
                continue;
            }

            // Every mass storage device has exactly two endpoints
            // One IN and one OUT endpoint
            int endpointCount = usbInterface.getEndpointCount();
            if (endpointCount != 2) {
                Log.w(TAG, "inteface endpoint count != 2");
            }

            UsbEndpoint outEndpoint = null;
            UsbEndpoint inEndpoint = null;
            for (int j = 0; j < endpointCount; j++) {
                UsbEndpoint endpoint = usbInterface.getEndpoint(j);
                Log.i(TAG, "found usb endpoint: " + endpoint);
                if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                        outEndpoint = endpoint;
                    } else {
                        inEndpoint = endpoint;
                    }
                }
            }

            if (outEndpoint == null || inEndpoint == null) {
                Log.e(TAG, "Not all needed endpoints found!");
                continue;
            }

            result = true;
        }

        return result;
    }


    /**
     * 获取单个文件的MD5值！
     * @param file file
     * @return String
     *
     *
     * 这个方法  0 开头的不行
     */
    public static String getFileMD5(File file,int i) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }

    /**
     * 获取文件夹中文件的MD5值
     * @param file file
     * @param listChild
     *            ;true递归子目录中的文件
     * @return
     */
    public static Map<String, String> getDirMD5(File file, boolean listChild) {
        if (!file.isDirectory()) {
            return null;
        }
        Map<String, String> map = new HashMap<String, String>();
        String md5;
        File files[] = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory() && listChild) {
                map.putAll(getDirMD5(f, listChild));
            } else {
                md5 = getFileMD5(f);
                if (md5 != null) {
                    map.put(f.getPath(), md5);
                }
            }
        }
        return map;
    }


    public static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return bytesToHexString(digest.digest());
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

}

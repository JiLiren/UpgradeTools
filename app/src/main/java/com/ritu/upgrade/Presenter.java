package com.ritu.upgrade;

import android.content.Context;
import android.hardware.usb.UsbManager;


/**
 * @author ritu on 14-Jun-18
 * */
public class Presenter {

    private Context context;
    private UsbManager mUsbManager;

    Presenter(Context context){
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

//    /**
//     * 枚举设备
//     */
//    private void enumerateDevice() {
//        if (myUsbManager == null)
//            return;
//
//        HashMap<String, UsbDevice> deviceList = myUsbManager.getDeviceList();
//        if (!deviceList.isEmpty()) { // deviceList不为空
//            StringBuffer sb = new StringBuffer();
//            for (UsbDevice device : deviceList.values()) {
//                sb.append(device.toString());
//                sb.append("\n");
//                info.setText(sb);
//                // 输出设备信息
//                Log.d(TAG, "DeviceInfo: " + device.getVendorId() + " , "
//                        + device.getProductId());
//
//                // 枚举到设备
//                if (device.getVendorId() == VendorID
//                        && device.getProductId() == ProductID) {
//                    myUsbDevice = device;
//                    Log.d(TAG, "枚举设备成功");
//                }
//            }
//        }
//    }
//
//    /**
//     * 找设备接口
//     */
//    private void findInterface() {
//        if (myUsbDevice != null) {
//            Log.d(TAG, "interfaceCounts : " + myUsbDevice.getInterfaceCount());
//            for (int i = 0; i < myUsbDevice.getInterfaceCount(); i++) {
//                UsbInterface intf = myUsbDevice.getInterface(i);
//                // 根据手上的设备做一些判断，其实这些信息都可以在枚举到设备时打印出来
//                if (intf.getInterfaceClass() == 8
//                        && intf.getInterfaceSubclass() == 6
//                        && intf.getInterfaceProtocol() == 80) {
//                    myInterface = intf;
//                    Log.d(TAG, "找到我的设备接口");
//                }
//                break;
//            }
//        }
//    }
//
//    /**
//     * 打开设备
//     */
//    private void openDevice() {
//        if (myInterface != null) {
//            UsbDeviceConnection conn = null;
//            // 在open前判断是否有连接权限；对于连接权限可以静态分配，也可以动态分配权限，可以查阅相关资料
//            if (myUsbManager.hasPermission(myUsbDevice)) {
//                conn = myUsbManager.openDevice(myUsbDevice);
//            }
//
//            if (conn == null) {
//                return;
//            }
//
//            if (conn.claimInterface(myInterface, true)) {
//                myDeviceConnection = conn; // 到此你的android设备已经连上HID设备
//                Log.d(TAG, "打开设备成功");
//            } else {
//                conn.close();
//            }
//        }
//    }
}

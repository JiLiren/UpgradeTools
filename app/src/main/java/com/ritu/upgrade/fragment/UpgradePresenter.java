package com.ritu.upgrade.fragment;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;

import java.io.File;
import java.io.IOException;


/**
 * @author ritu on 14-Jun-18
 * */
public class UpgradePresenter {

    private final String TAG = "升级工具";

    private UpgradeView mView;

    private String sdPath;

    UpgradePresenter(UpgradeView mView) {
        this.mView = mView;
    }

    /**
     * 获取SD卡目录文件
     * */
    public File getFile(){
        File sdDir = null;
        // 判断sd卡是否存在
        boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            // 获取跟目录
            sdDir = Environment.getExternalStorageDirectory();
            if (sdPath == null){
                sdPath = sdDir.getAbsolutePath();
            }
        }else {
            Toast.makeText(mView.getContext(),"没有搜索到SD卡",Toast.LENGTH_SHORT).show();
            return null;
        }
        return sdDir;
    }

    public String getSDString() {
        return sdPath;
    }

    public void getUsbDevice(){
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(mView.getContext());
        for (UsbMassStorageDevice device:devices){
            try {
                //在与设备进行交互之前，您需要调用init（）！
                device.init();
                //只使用设备上的第一个分区
                FileSystem currentFs = device.getPartitions().get(0).getFileSystem();
                Log.d(TAG ,"Capacity:"+currentFs.getCapacity());
//                Log.d(TAG,"占用空间："+ currentFs.getOc​​cupiedSpace());
                Log.d(TAG,"Free Space："+ currentFs.getFreeSpace());
                Log.d(TAG,"Chunk size："+ currentFs.getChunkSize());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

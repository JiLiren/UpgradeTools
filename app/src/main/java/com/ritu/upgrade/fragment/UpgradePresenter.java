package com.ritu.upgrade.fragment;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.ritu.upgrade.event.OnCopyListener;
import com.ritu.upgrade.tools.EncryptionTools;
import com.ritu.upgrade.tools.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

    /**
     * 获取配置的键值对
     * */
    public Map<String,String> getConfigValue(File file){
        Map<String,String> map = new HashMap<>();
        if (file.isDirectory()) {
            Log.d("TestFile", "The File doesn't not exist.");
        } else {
            try {
                InputStream instream = new FileInputStream(file);
                if (instream != null) {
                    InputStreamReader inputreader = new InputStreamReader(instream);
                    BufferedReader buffreader = new BufferedReader(inputreader);
                    String line;
                    String key;
                    String value;
                    //分行读取
                    while (( line = buffreader.readLine()) != null) {
                        if (line.contains("=")){
                            key = line.substring(0,line.indexOf("="));
                            value = line.substring(line.indexOf("=")+1,line.length());
                            map.put(key,value);
                        }
                    }
                    instream.close();
                }
            } catch (java.io.FileNotFoundException e) {
                Log.d("TestFile", "The File doesn't not exist.");
            } catch (IOException e) {
                Log.d("TestFile", e.getMessage());
            }
        }
        return map;
    }

    /**
     * 拷贝
     * */
    public void copy(String root, String toFile, OnCopyListener listener){
        new CopyTask(root,toFile,listener).execute();
    }

    /**
     * 拷贝
     * @param root 来源文件
     * */
    private void copyFile(File root, String toFile,long number,long cur,Map<String,String> map,
            NumberFormat format,OnCopyListener listener) {
        Log.e("1111111number","---------------------++++++++++++++++++++++"+number);
        //要复制的文件目录
        File[] currentFiles;
        //如同判断SD卡是否存在或者文件是否存在
        //如果不存在则 return出去
        if(!root.exists()) {
            return;
        }
        //如果存在则获取当前目录下的全部文件 填充数组
        currentFiles = root.listFiles();
        //目标目录
        File targetDir = new File(toFile);
        //创建目录
        if(!targetDir.exists()) {
            targetDir.mkdirs();
        }
        Exception exception;
        String result;
        String schedule;
        //遍历要复制该目录下的全部文件
        for (File currentFile : currentFiles) {
            //如果当前项为子目录 进行递归
            if (currentFile.isDirectory()) {
                copyFile(
                        new File(currentFile.getPath() + "/"), toFile + currentFile.getName() + "/",
                        number,cur,map,format,listener
                );
                //如果当前项为文件则进行文件拷贝
            } else {
                exception = copySdcardFile(currentFile.getPath(), toFile + currentFile.getName());
                result = exception == null ? "成功" : exception.getMessage();
                map.put(currentFile.getPath(),result);
                cur ++;
                Log.e("1111111","---------------------"+cur);
                schedule = format.format((float) cur / (float) number * 100);
                if (listener != null){
                    listener.onProgressUpdate(schedule);
                }
            }
        }
    }

    /**
     * 文件拷贝
     * 要复制的目录下的所有非子目录(文件夹)文件拷贝
     * @param fromFile from
     * @param toFile to
     * */
    private Exception copySdcardFile(String fromFile, String toFile) {
        try {
            InputStream fosfrom = new FileInputStream(fromFile);
            OutputStream fosto = new FileOutputStream(toFile);
            byte bt[] = new byte[1024];
            int c;
            while ((c = fosfrom.read(bt)) > 0) {
                fosto.write(bt, 0, c);
            }
            fosfrom.close();
            fosto.close();
            return null;
        } catch (Exception ex) {
            return ex;
        }
    }

    /**
     * 获取目录中有多少个文件
     * @param file file
     * */
    private long getFileNumber(File file){
        long number = 0;
        if (file == null){
            return number;
        }
        if(!file.exists()) {
            return number;
        }
        if (file.isFile()){
            return 1;
        }
        if (file.isDirectory()){
            for (File childFile : file.listFiles()){
                number += getFileNumber(childFile);
            }
        }
        return number;
    }

    /**
     * 复制 校验 等等一系列任务的 线程池
     * */
    @SuppressLint("StaticFieldLeak")
    private class CopyTask extends AsyncTask<Void, Integer, Void> {
        private String root;
        private String toFile;
        private OnCopyListener listener;

        CopyTask(String root, String toFile, OnCopyListener listener) {
            this.root = root;
            this.toFile = toFile;
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            File file = new File(root);
            long number = getFileNumber(file);
            long curNumber = 0;
            Map<String,String> upgradeInformation = new HashMap<>();
            NumberFormat numberFormat = NumberFormat.getInstance();
            numberFormat.setMaximumFractionDigits(2);
            copyFile(file,toFile,number,curNumber,upgradeInformation,numberFormat,listener);
            if (listener != null){
                listener.onFinish(upgradeInformation);
            }
            Map<String,String> hashMap = getCheckHash(file);
            File orgFile = null;
            if (listener != null){
                orgFile = new File(listener.onCheckStart());
            }
            if (orgFile == null || !orgFile.exists()){
                return null;
            }
            Map<String,String> checkMap = getCheckValue(orgFile);
            String oValue;
            String hValue;
            for (String key : checkMap.keySet()) {
                oValue = checkMap.get(key);
                hValue = hashMap.get(key);
                if (oValue != null){
                    if (!oValue.equals(hValue)){
                        listener.onCheckFinish(upgradeInformation);
                            return null;
                    }
                }
            }
            listener.onCheckFinish(null);
            return null;
        }

        /**
         * MD5校验
         * 获取复制后文件的MD5 值
         * @param file file
         * */
        private Map<String,String> getCheckHash(File file ){
            Map<String,String> hashMap = new HashMap<>();
            if (file.isDirectory()){
                for (File childFile : file.listFiles()){
                    hashMap.putAll(getCheckHash(childFile));
                }
            }else if (file.isFile()){
                String path = file.getAbsolutePath();
                String newPath = path.replaceAll(root,toFile);
                String key = path.replaceAll(root,"/");
                String value ;
                File f = new File(newPath);
                if (!f.exists()){
                    value = "";
                }else {
                    value = FileUtils.getFileMD5(file);
                    Log.e("111","1111");
                }
                hashMap.put(key,value);
            }
            return hashMap;
        }

        /**
         * 获取配置的键值对
         * */
        private Map<String,String> getCheckValue(File file){
            Map<String,String> map = new HashMap<>();
            if (file.isDirectory()) {
                Log.d("TestFile", "The File doesn't not exist.");
            } else {
                try {
                    InputStream instream = new FileInputStream(file);
                    if (instream != null) {
                        InputStreamReader inputreader = new InputStreamReader(instream);
                        BufferedReader buffreader = new BufferedReader(inputreader);
                        String line;
                        String key;
                        String key1;
                        String value;
                        //分行读取
                        while (( line = buffreader.readLine()) != null) {
                            if (line.contains(",")){
                                key1 = line.substring(0,line.indexOf(","));
                                key = key1.replace("\\","/");
                                value = line.substring(line.indexOf(",")+1,line.length());
                                map.put(key,value);
                            }
                        }
                        instream.close();
                    }
                } catch (java.io.FileNotFoundException e) {
                    Log.d("TestFile", "The File doesn't not exist.");
                } catch (IOException e) {
                    Log.d("TestFile", e.getMessage());
                }
            }
            return map;
        }
    }

}

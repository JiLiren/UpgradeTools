package com.ritu.upgrade.fragment;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.ritu.upgrade.event.OnCopyListener;
import com.ritu.upgrade.tools.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;


/**
 * @author ritu on 14-Jun-18
 * */
public class UpgradePresenter {

    private final String TAG = "升级工具";
    private static final String ACTION_USB_PERMISSION = "com.androidinspain.otgviewer.USB_PERMISSION";

    private UpgradeView mView;
    private UsbFile usbRoot;

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
        if (sdPath == null){
            boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
            if (sdCardExist) {
                File sdDir = Environment.getExternalStorageDirectory();
                sdPath = sdDir.getAbsolutePath();
            }
        }
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

    public UsbFile getUsbRootFile() {
        return usbRoot;
    }

    public UsbFile getUsbConfigView(){
        UsbManager usbManager = (UsbManager) mView.getContext().getSystemService(Context.USB_SERVICE);
        if (usbManager == null){
            mView.onToast("没有U盘");
            return null;
        }
        //枚举设备
        //获取存储设备
//        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(getActivity());
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(mView.getContext());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mView.getContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        UsbMassStorageDevice storageDevice = null;

        if (devices.length > 0) {
            storageDevice = devices[0];
        }else {
            mView.onToast("没有U盘");
            return null;
        }
        try {
            if (usbManager.hasPermission(storageDevice.getUsbDevice())) {
//            if (usbManager.hasPermission(storageDevice.getUsbDevice())) {
                storageDevice.init();
                FileSystem fs = storageDevice.getPartitions().get(0).getFileSystem();
                mView.onToast("拿到U盘");
                UsbFile root = fs.getRootDirectory();
                for (UsbFile file : root.listFiles()) {
                    if ("myconfig.ini".equals(file.getName())) {
                        this.usbRoot = root;
                        return file;
                    }
                }
            }else {
                mView.onToast("没有权限");
                usbManager.requestPermission(storageDevice.getUsbDevice(), pendingIntent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

    public Map<String,String> getConfigValue(UsbFile file){
        Map<String,String> map = new HashMap<>();
        if (file.isDirectory()){
            Log.d("TestFile", "The File doesn't not exist.");
        }else {
            //读取文件内容
            InputStream is = new UsbFileInputStream(file);
            //读取秘钥中的数据进行匹配
            StringBuilder sb = new StringBuilder();
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(is));
                String line;
                String key;
                String value;
                //分行读取
                while (( line = bufferedReader.readLine()) != null) {
                    if (line.contains("=")){
                        key = line.substring(0,line.indexOf("="));
                        value = line.substring(line.indexOf("=")+1,line.length());
                        map.put(key,value);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
     * */
    public void copyUSB(String root, String toFile,OnCopyListener listener){
        new CopyUsbTask(root,toFile,listener).execute();
    }

    /**
     * 拷贝
     * @param root 来源文件
     * */
    private void copyFile(File root, String toFile,long number,long cur,Map<String,String> map,OnCopyListener listener) {
        //要复制的文件目录
        File[] currentFiles;
        //如同判断SD卡是否存在或者文件是否存在
        //如果不存在则 return出去
        if(!root.exists()) {
            listener.onToast(root.getName()+"root文件不存在");
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
                        number,cur,map,listener
                );
                //如果当前项为文件则进行文件拷贝
            } else {
                exception = copySdcardFile(currentFile.getPath(), toFile + currentFile.getName(),listener);
                result = exception == null ? "成功" : exception.getMessage();
                map.put(currentFile.getPath(),result);
                if (listener != null){
                    listener.onProgressUpdate();
                }
            }
        }
    }

    /**
     * 拷贝
     * @param root 来源文件
     * */
    private void copyUSBFile(UsbFile root, String toFile,long number,long cur,Map<String,String> map,
                             Map<String,String> hashRoot,OnCopyListener listener) throws IOException {
        toFile = toFile.replaceAll("//","/");
        //要复制的文件目录
        UsbFile[] currentFiles;
        //如同判断SD卡是否存在或者文件是否存在
        //如果不存在则 return出去
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
        for (UsbFile currentFile : currentFiles) {
            //如果当前项为子目录 进行递归
            if (currentFile.isDirectory()) {
                String tr = toFile + "/"+currentFile.getName();
                copyUSBFile(currentFile,tr + "/",
                        number,cur,map,hashRoot,listener);
                //如果当前项为文件则进行文件拷贝
            } else {
                String tr = toFile + currentFile.getName();

                exception = copySdcardFile(currentFile, tr,hashRoot,listener);
                result = exception == null ? "成功" : exception.getMessage();
                int s = tr.indexOf("RtNavi") + 6;
                int e = tr.length();
                String key= tr.substring(s,e);
                map.put(key ,result);
                if (listener != null){
                    listener.onProgressUpdate();
                }
            }
        }
    }


    private String getUSBFile2SDPath(UsbFile file,String path){
        if ("RtNavi".equals(file.getName())){
            return "/RtNavi"+path;
        }else {
            return getUSBFile2SDPath(file.getParent(),"/"+file.getName()+path);
        }
    }

    /**
     * 文件拷贝
     * 要复制的目录下的所有非子目录(文件夹)文件拷贝
     * @param fromFile from
     * @param toFile to
     * */
    private Exception copySdcardFile(String fromFile, String toFile,OnCopyListener listener) {
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
            listener.onToast(ex.getMessage()+"拷贝异常"+toFile);
            return ex;
        }
    }

    private Exception copySdcardFile(UsbFile fromFile, String toFile, Map<String,String> hashRoot,
                                     OnCopyListener listener) {
        try {
            toFile = toFile.replaceAll("//","/");
            InputStream fosfrom = new UsbFileInputStream(fromFile);
            OutputStream fosto = new FileOutputStream(toFile);
            byte bt[] = new byte[1024];
            int c;
            while ((c = fosfrom.read(bt)) > 0) {
                fosto.write(bt, 0, c);
            }
            fosfrom.close();
            fosto.close();
            String hashValue = FileUtils.getFileMD5(new File(toFile));

            int s = toFile.indexOf("RtNavi") + 6;
            int e = toFile.length();
            String key= toFile.substring(s,e);
            key = key.replaceAll("/","\\\\");
            key = key.replace(".","_");
            hashRoot.put(key,hashValue);
            return null;
        } catch (Exception ex) {
            listener.onToast(ex.getMessage()+"拷贝异常"+toFile);
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
     * 获取目录中有多少个文件
     * @param file file
     * */
    private long getFileNumber(UsbFile file) throws IOException {
        long number = 0;
        if (file == null){
            return number;
        }
        if (file.isDirectory()){
            for (UsbFile childFile : file.listFiles()){
                number += getFileNumber(childFile);
            }
        }else {
            return 1;
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
            long total = getFileNumber(file);
            long curNumber = 0;
            Map<String,String> upgradeInformation = new HashMap<>();
            if (listener != null){
                listener.onStart(total);
            }
            copyFile(file,toFile,total,curNumber,upgradeInformation,listener);
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
            writeFile(upgradeInformation);
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
                }else {
                    boolean b = true;
                    for (String sk:hashMap.keySet()){
                        if (sk.equalsIgnoreCase(key)){
                            oValue = checkMap.get(key);
                            hValue = hashMap.get(key);
                            if (!oValue.equals(hValue)){
                                b = false;
                            }
                        }
                    }
                    if (b){
                        listener.onCheckFinish(upgradeInformation);
                        return null;
                    }
                }
            }
            listener.onCheckFinish(null);
            return null;
        }

        private void writeFile( Map<String,String> map ){
            if (map == null){
                return;
            }
            File file = makeFilePath();
            if (file == null){
                return;
            }
            StringBuilder buffer = new StringBuilder();
            String value;
            for (String key : map.keySet()) {
                value = map.get(key);
                buffer.append(key);
                buffer.append(" = ");
                buffer.append(value);
                buffer.append("\r\n");
                System.out.println(key + ":" + map.get(key));
            }
            String msg = buffer.toString();
            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                writer.write(msg);
                writer.close();
            } catch(Exception exception) {
                Log.d(TAG,exception.getMessage());
            }
        }

        private File makeFilePath(){
            File file = new File(getSDString() + "/UpdateLog.txt");
            if (!file.exists()){
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return file;
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

    @SuppressLint("StaticFieldLeak")
    private class CopyUsbTask extends AsyncTask<Void, Integer, Void> {
        private String root;
        private String toFile;
        private OnCopyListener listener;

        private UsbFile rFile;

        CopyUsbTask(String root, String toFile,OnCopyListener listener) {
            this.root = root;
            this.toFile = toFile;
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                UsbFile file = searchSourceFile(usbRoot, root);
                rFile = file;
                long total = getFileNumber(file);
                long curNumber = 0;
                Map<String,String> hashRoot= new HashMap<>();
                Map<String, String> upgradeInformation = new HashMap<>();
                if (listener != null) {
                    listener.onStart(total);
                }
                if (file == null){
                    return null;
                }
                if (!toFile.contains("RtNavi")){
                    toFile += "RtNavi/";
                }
                copyUSBFile(file, toFile, total, curNumber, upgradeInformation,hashRoot, listener);
                if (listener != null){
                    listener.onFinish(upgradeInformation);
                }
                UsbFile orgFile = null;
                if (listener != null){
                    String name = listener.onCheckUSBStart();
                    for (UsbFile f : rFile.listFiles()){
                        if (name.equals(f.getName())){
                                orgFile = f;
                        }
                    }
                }
                if (orgFile == null ){
                    return null;
                }
                writeFile2(hashRoot);
                writeFile(upgradeInformation);
                Map<String,String> checkMap = getCheckUSBValue(orgFile);
                String oValue;
                String hValue;
                String key1;
                for (String key : checkMap.keySet()) {
                    key1 = key.replace(".","_");
                    oValue = checkMap.get(key);
                    hValue = hashRoot.get(key1);
                    if (oValue != null){
                        if (!oValue.equals(hValue)){
                            listener.onCheckFinish(upgradeInformation);
                            return null;
                        }else {
                            try {
                                boolean b = true;
                                for (String sk:hashRoot.keySet()){
                                    if (sk.equalsIgnoreCase(key1)){
                                        hValue = checkMap.get(key1);
                                        if (!oValue.equals(hValue)){
                                            b = false;
                                        }
                                    }
                                }

                                if (b){
                                    listener.onCheckFinish(upgradeInformation);
                                    return null;
                                }
                            }catch (Exception e){
                                mView.onToast(e.getMessage());
                            }
                        }
                    }else {

                    }
                }
            listener.onCheckFinish(null);
            }catch (IOException e) {
                mView.onToast(e.getMessage());
            }
            return null;
        }

        private UsbFile getHashFine(UsbFile file,String name) throws IOException {
            for (UsbFile f:file.listFiles()){
                if (f.isDirectory()){
                    for (UsbFile ff : f.listFiles()){
                        getHashFine(ff,name);
                    }
                }else {
                    if (name.equals(f.getName())){
                       return f;
                    }
                }
            }
            return null;
        }

        private void writeFile2( Map<String,String> map ){
            if (map == null){
                return;
            }
            File file = makeFilePath1();
            if (file == null){
                return;
            }
            StringBuilder buffer = new StringBuilder();
            String value;
            for (String key : map.keySet()) {
                value = map.get(key);
                buffer.append(key);
                buffer.append(" = ");
                buffer.append(value);
                buffer.append("\r\n");
                System.out.println(key + ":" + map.get(key));
            }
            String msg = buffer.toString();
            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                writer.write(msg);
                writer.close();
            } catch(Exception exception) {
                Log.d(TAG,exception.getMessage());
            }
        }

        private void writeFile( Map<String,String> map ){
            if (map == null){
                return;
            }
            File file = makeFilePath();
            if (file == null){
                return;
            }
            StringBuilder buffer = new StringBuilder();
            String value;
            for (String key : map.keySet()) {
                value = map.get(key);
                buffer.append(key);
                buffer.append(" = ");
                buffer.append(value);
                buffer.append("\r\n");
                System.out.println(key + ":" + map.get(key));
            }
            String msg = buffer.toString();
            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                writer.write(msg);
                writer.close();
            } catch(Exception exception) {
                Log.d(TAG,exception.getMessage());
            }
        }

        private File makeFilePath(){
            File file = new File(getSDString() + "/UpdateLog.txt");
            if (!file.exists()){
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return file;
        }

        private File makeFilePath1(){
            File file = new File(getSDString() + "/UpdateLog1.txt");
            if (!file.exists()){
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return file;
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

        private Map<String,String> getCheckUSBValue(UsbFile file){
            Map<String,String> map = new HashMap<>();
            if (file.isDirectory()) {
                Log.d("TestFile", "The File doesn't not exist.");
            } else {
                try {
                    InputStream instream = new UsbFileInputStream(file);
                    if (instream != null) {
                        InputStreamReader inputreader = new InputStreamReader(instream);
                        BufferedReader buffreader = new BufferedReader(inputreader);
                        String line;
                        String key;
                        String value;
                        //分行读取
                        while (( line = buffreader.readLine()) != null) {
                            if (line.contains(",")){
                                key = line.substring(0,line.indexOf(","));
//                                key = key1.replace("\\","/");
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


        private UsbFile searchSourceFile(UsbFile file,String path) throws IOException {
            if (TextUtils.isEmpty(path) || file == null ){
                return null;
            }
//            if (!path.startsWith("/")){
                String filePath = path.substring(1,path.length());
                int index = filePath.indexOf("/");
                if (filePath.indexOf("/") > 0){
                    index = filePath.indexOf("/");
                }
                String fileName = filePath.substring(0,index > 0 ? index : filePath.length());
                for (UsbFile f : file.listFiles()){
                    if (fileName.equals(f.getName())){
                        if (index > 0 ){
                            return searchSourceFile(f,filePath.substring(index,filePath.length()));
                        }else {
                            return f;
                        }
                    }
                }
//            }
            return null;
        }
    }

}

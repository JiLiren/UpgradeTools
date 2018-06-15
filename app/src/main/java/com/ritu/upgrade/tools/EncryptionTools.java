package com.ritu.upgrade.tools;

/**
 * @author ritu on 14-Jun-18
 * */
public class EncryptionTools {

    static {
        System.loadLibrary("GetFileMD5");
    }

    /**
     * getEncryptionData
     * GetFileMD5(JNIEnv* env, jobject thiz, jstring jstrFileName)
     * */
    public static native String GetFileMD5(String timestamp);



}

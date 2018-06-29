package com.ritu.upgrade.event;


import java.util.Map;

/**
 * @author ritu on 19-Jun-18
 * */
public interface OnCopyListener {
    /**
     * 开始
     * @param total total
     * */
    void onStart(long total);
    /**
     * 拷贝状态
     * */
    void onProgressUpdate();
    /**
     * 完成
     * @param map map
     * */
    void onFinish(Map<String,String> map);
    /**
     * 开始校验
     * @return 校验文件路径
     * */
    String onCheckStart();

    String onCheckUSBStart();
    /**
     * 完成
     * @param map map
     * */
    void onCheckFinish(Map<String,String> map);

    void onToast(String msg);
}

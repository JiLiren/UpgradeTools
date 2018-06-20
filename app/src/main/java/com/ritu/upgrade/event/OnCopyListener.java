package com.ritu.upgrade.event;


import java.util.Map;

/**
 * @author ritu on 19-Jun-18
 * */
public interface OnCopyListener {
    /**
     * 开始
     * */
    void onStart();
    /**
     * 拷贝状态
     * @param schedule 0-100 进度
     * */
    void onProgressUpdate(String schedule);
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
    /**
     * 完成
     * @param map map
     * */
    void onCheckFinish(Map<String,String> map);
}

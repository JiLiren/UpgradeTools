package com.ritu.upgrade.fragment;

import android.content.Context;
import android.os.Message;

/**
 * @author ritu on 14-Jun-18
 * */
public interface UpgradeView {

    Context getContext();

    void onToast(String msg);
}

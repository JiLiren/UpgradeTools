package com.ritu.upgrade.fragment;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TableRow;
import android.widget.TextView;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.fat32.FatFile;
import com.github.mjdev.libaums.partition.Partition;
import com.ritu.upgrade.MainActivity;
import com.ritu.upgrade.R;
import com.ritu.upgrade.adp.FileAdapter;
import com.ritu.upgrade.adp.OnItemClickListener;
import com.ritu.upgrade.event.OnCopyListener;
import com.ritu.upgrade.tools.ScreenUtil;
import com.ritu.upgrade.widget.ScrollLayout;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * @author ritu on 14-Jun-18
 * */
public class UpgradeFragment extends Fragment implements UpgradeView,OnMenuItemClickListener,
        ScrollLayout.OnScrollChangedListener,OnItemClickListener{

    /** 开始拷贝 */
    private final int MSG_COPY_START = 0x0001;
    /** 拷贝进度 */
    private final int MSG_COPY_UPDATE = 0x0002;
    /** 拷贝结束 */
    private final int MSG_COPY_FINISH = 0x0003;
    /** 校验开始 */
    private final int MSG_CHECK_START = 0x0004;
    /** 校验结束 */
    private final int MSG_CHECK_FINISH = 0x0005;

    private boolean isUpgrade;
    private Button mUpgradeBtn;
    private TextView mSourceView;
    private TextView mMsgView;
    private TextView mPathView;
    private ScrollLayout mScrollLayout;
    private ListView mListView;
    private LinearLayout mScrollContentLayout;
    private NumberProgressBar mBar;
    private boolean isOTG;

    private File mConfigFile;
    private UsbFile mUsbConfigFile;
    private Map<String,String> mConfigMap;

    private PopupMenu mSourceMenu;
    private UpgradePresenter mPresenter;
    private MainHandler mHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_launcher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPresenter = new UpgradePresenter(this);
        intiView(view);
        initEvent();
    }

    private void intiView(View view){
        mUpgradeBtn = view.findViewById(R.id.btn_up);
        mMsgView = view.findViewById(R.id.tv_msg);
        mSourceView = view.findViewById(R.id.tv_source);
        mListView = view.findViewById(R.id.list_view);
        mPathView = view.findViewById(R.id.tv_path);
        mScrollLayout = view.findViewById(R.id.scroll_down_layout);
        mScrollContentLayout = view.findViewById(R.id.layout_scroll_content);
        mBar = view.findViewById(R.id.ProgressBar);

        mScrollLayout.setMinOffset(0);
        if (getActivity() != null) {
            mScrollLayout.setMaxOffset((int) (ScreenUtil.getScreenHeight(getActivity()) * 0.5));
        }
        if (getContext() != null) {
            mScrollLayout.setExitOffset(ScreenUtil.dip2px(getContext(), 50));
        }
        mScrollLayout.setIsSupportExit(true);
        mScrollLayout.setAllowHorizontalScroll(true);
        mScrollLayout.setOnScrollChangedListener(this);
        mScrollLayout.setToExit();

        mScrollLayout.getBackground().setAlpha(0);

        setSource(getString(R.string.usb));

        mHandler = new MainHandler(this);
    }

    private void initEvent(){
        mUpgradeBtn.setOnClickListener(v -> {
            isUpgrade = true;
            isOTG = true;
            mUpgradeBtn.setEnabled(false);
            startUpdate();
        });

        mSourceView.setOnClickListener(v ->{
            showSwitchSource();
        });

    }

    private void startUpdate(){
        mUsbConfigFile = mPresenter.getUsbConfigView();
        if (mUsbConfigFile == null){
            mUpgradeBtn.setEnabled(true);
            return;
        }
        mConfigMap = mPresenter.getConfigValue(mUsbConfigFile);
        copyFileUSB();
    }

    /**
     * 选择升级来源
     * */
    private void showSwitchSource(){
        if (mSourceMenu == null){
            mSourceMenu = new PopupMenu(getContext(), mSourceView);
            MenuInflater inflater = mSourceMenu.getMenuInflater();
            inflater.inflate(R.menu.menu_source, mSourceMenu.getMenu());
            mSourceMenu.setOnMenuItemClickListener(this);
        }
        mSourceMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.usb:
                mSourceView.setText(getString(R.string.usb));
                return true;
            case R.id.sdcard:
                mScrollContentLayout.setVisibility(View.VISIBLE);
                mScrollLayout.setToOpen();
                setAdapter(mPresenter.getFile());
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onScrollProgressChanged(float currentProgress) {
        if (currentProgress >= 0) {
            float precent = 255 * currentProgress;
            if (precent > 255) {
                precent = 255;
            } else if (precent < 0) {
                precent = 0;
            }
            mScrollLayout.getBackground().setAlpha(255 - (int) precent);
        }
    }

    @Override
    public void onScrollFinished(ScrollLayout.Status currentStatus) {
        if (currentStatus.equals(ScrollLayout.Status.EXIT)) {
            mScrollContentLayout.setVisibility(View.GONE);
        }
    }

    private void setSource(String source){
        mSourceView.setText(source);
    }

    @Override
    public void onChildScroll(int top) {

    }

    private void setAdapter(File file) {
        mPathView.setText(file.getAbsolutePath());
        if (file.isDirectory()) {
            List<File> listSource = Arrays.asList(file.listFiles());
            List<File> list = new ArrayList<>();
            boolean haseParent = false;
            if (!mPresenter.getSDString().equals(file.getAbsolutePath())){
                list.add(file.getParentFile());
                haseParent = true;
            }
            for (File bean:listSource){
                if (!bean.isFile()){
                    list.add(bean);
                }else {
                    if ("myconfig.ini".equals(bean.getName())){
                        list.add(bean);
                    }
                }
            }
            mListView.setAdapter(new FileAdapter(getContext(),this,haseParent,list));
        }
    }

    @Override
    public void onItemClick(File file) {
        if (file == null){
            return;
        }
        if (file.isDirectory()){
            setAdapter(file);
        }else if (file.isFile()){
            mConfigFile = file;
            isOTG = false;
            setSource(getString(R.string.sdcard));
            mScrollLayout.scrollToExit();
            mConfigMap = mPresenter.getConfigValue(mConfigFile);
            mUpgradeBtn.setEnabled(false);
            copyFile();
        }
    }

    private void copyFile(){
        String filePath = mConfigMap.get("filename").replace("\\","/");

        if (TextUtils.isEmpty(filePath)){
            return;
        }
        String sourcePath = mPresenter.getSDString() + filePath + "NAVIGATION/";

        String aimsFilePath = mConfigMap.get("path").replace("\\","/");
        if (TextUtils.isEmpty(filePath)){
            return;
        }
        String aimsPath = mPresenter.getSDString() + aimsFilePath;
        mPresenter.copy(sourcePath,aimsPath,mListener);
    }

    private void copyFileUSB(){
        String filePath = mConfigMap.get("filename").replace("\\","/");
        UsbFile rootFile  = null;
        if (mPresenter != null){
            rootFile = mPresenter.getUsbRootFile();
        }
        if (TextUtils.isEmpty(filePath)){
            return;
        }
        if (rootFile == null){
            return;
        }
        String sourcePath = rootFile.getName() + filePath + "NAVIGATION/";
        String aimsFilePath = mConfigMap.get("path").replace("\\","/");
        if (TextUtils.isEmpty(filePath)){
            return;
        }
        String aimsPath = mPresenter.getSDString() + aimsFilePath;
        mPresenter.copy(sourcePath,aimsPath,mListener);
    }


    @SuppressLint("HandlerLeak")
    private class MainHandler extends Handler{

        WeakReference<UpgradeFragment> mFragment;

        MainHandler(UpgradeFragment fragment) {
            mFragment = new WeakReference<UpgradeFragment>(fragment);
        }
        @Override
        public void handleMessage(Message msg) {
            UpgradeFragment fragment = mFragment.get();
            if (fragment == null){
                return;
            }
            switch (msg.what){
                case MSG_COPY_START:
                    mMsgView.setText("正在拷贝升级包...");
                    mBar.setVisibility(View.VISIBLE);
                    break;
                case MSG_COPY_UPDATE:
                    float proportion = (float) msg.obj;
                    mBar.setProgress((int) proportion);
                    break;
                case MSG_COPY_FINISH:
                    mMsgView.setText("正在校验升级包...");
                    mBar.setVisibility(View.GONE);
                    break;
                case MSG_CHECK_START:
                    mMsgView.setText("正在校验升级包...");
                    break;
                case MSG_CHECK_FINISH:
                    boolean bug = (boolean) msg.obj;
                    String text;
                    if (bug){
                        mUpgradeBtn.setEnabled(true);
                        text = "导航新数据安装升级失败，请重启设备后重新升级！";
                    }else {
                        text = "导航新数据安装升级成功，请重启设备后重新升级！";
                    }
                    mMsgView.setText(text);
                    break;
                default:break;
            }
        }
    }

    /**
     * 拷贝接口
     * */
    private OnCopyListener mListener = new OnCopyListener() {

        private long total;
        private long count;
        @Override
        public void onStart(long total) {
            count = 0;
            this.total = total;
            Message message = new Message();
            message.what = MSG_COPY_START;
            mHandler.sendMessage(message);
        }

        @Override
        public synchronized void onProgressUpdate() {
            count ++;
            float proportion = (float) count / (float) total * 100;
            Message message = new Message();
            message.what = MSG_COPY_UPDATE;
            message.obj = proportion;
            mHandler.sendMessage(message);
        }

        @Override
        public void onFinish(Map<String, String> map) {
            Message message = new Message();
            message.what = MSG_COPY_FINISH;
            mHandler.sendMessage(message);
        }

        @Override
        public String onCheckStart() {
            Message message = new Message();
            message.what = MSG_CHECK_START;
            mHandler.sendMessage(message);
            String path = mPresenter.getSDString() +
                    mConfigMap.get("filename").replace("\\","/") +
                    "NAVIGATION" + "/" +
                    mConfigMap.get("hashfile").replace("\\","/");
            return path;
        }

        @Override
        public void onCheckFinish(Map<String, String> map) {
            Message message = new Message();
            message.what = MSG_CHECK_FINISH;
            message.obj = map != null;
            mHandler.sendMessage(message);
        }
    };


}

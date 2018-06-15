package com.ritu.upgrade.fragment;

import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ritu.upgrade.R;
import com.ritu.upgrade.adp.FileAdapter;
import com.ritu.upgrade.adp.OnItemClickListener;
import com.ritu.upgrade.tools.EncryptionTools;
import com.ritu.upgrade.tools.FileUtils;
import com.ritu.upgrade.tools.ScreenUtil;
import com.ritu.upgrade.widget.ScrollLayout;
import com.ritu.upgrade.widget.span.SpannablePair;
import com.ritu.upgrade.widget.span.SpannableTextView;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author ritu on 14-Jun-18
 * */
public class UpgradeFragment extends Fragment implements UpgradeView,OnMenuItemClickListener,
        ScrollLayout.OnScrollChangedListener,OnItemClickListener{

    private boolean isUpgrade;

    private Button mUpgradeBtn;
    private TextView mSourceView;
    private TextView mMsgView;
    private TextView mPathView;
    private ScrollLayout mScrollLayout;
    private ListView mListView;
    private LinearLayout mScrollContentLayout;
    private boolean isOTG;

    private File mConfigFile;

    private PopupMenu mSourceMenu;
    private UpgradePresenter mPresenter;

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


        mSourceView.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);

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
    }

    private void initEvent(){
        mUpgradeBtn.setOnClickListener(v -> {
            isUpgrade = true;
            mUpgradeBtn.setEnabled(false);
        });

        mSourceView.setOnClickListener(v ->{
            showSwitchSource();
        });

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
            mSourceView.setText(getString(R.string.sdcard));
            mScrollLayout.scrollToExit();
//            String md5 =FileUtils.getFileMD5(file);
        }
    }

}

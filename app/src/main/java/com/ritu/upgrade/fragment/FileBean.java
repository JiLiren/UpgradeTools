package com.ritu.upgrade.fragment;

import java.util.ArrayList;
import java.util.List;

class FileBean {

    private boolean isFileOrDirectory;

    private List<FileBean> childBean = new ArrayList<>();

    private String path;


    public boolean isFileOrDirectory() {
        return isFileOrDirectory;
    }

    public void setFileOrDirectory(boolean fileOrDirectory) {
        isFileOrDirectory = fileOrDirectory;
    }

    public List<FileBean> getChildBean() {
        return childBean;
    }

    public void setChildBean(List<FileBean> childBean) {
        this.childBean = childBean;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

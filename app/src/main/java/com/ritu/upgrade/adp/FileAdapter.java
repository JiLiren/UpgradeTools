package com.ritu.upgrade.adp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ritu.upgrade.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ritu on 14-Jun-18
 * */
public class FileAdapter extends BaseAdapter {

    private List<File> files = new ArrayList<>();
    private Context context;
    private OnItemClickListener mListener;
    private boolean hasParent;

    public FileAdapter( Context context,OnItemClickListener listener,boolean hasParent,List<File> files) {
        this.context = context;
        this.mListener = listener;
        this.hasParent = hasParent;
        this.files = files;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public File getItem(int position) {
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder  viewHolder =null;
        if(convertView == null){
            convertView = View.inflate(context, R.layout.item_file,null);
            viewHolder = new ViewHolder();
            viewHolder.typeView = convertView.findViewById(R.id.iv_type);
            viewHolder.nameView = convertView.findViewById(R.id.tv_name);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }
        File file = getItem(position);
        if (file != null){
            viewHolder.typeView.setSelected(file.isFile());
            if (hasParent && position == 0){
                viewHolder.nameView.setText("..");
            }else {
                viewHolder.nameView.setText(file.getName());
            }

        }
        convertView.setOnClickListener(v -> {
            if (mListener != null){
                mListener.onItemClick(file);
            }
        });
        return convertView;
    }


    private static class ViewHolder{
        ImageView typeView;
        TextView nameView;
    }
}

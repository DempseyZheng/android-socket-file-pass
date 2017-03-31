package com.accvmedia.mysocket;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.accvmedia.mysocket.bean.TaskBean;

import java.util.ArrayList;

/**
 * Created by dempseyZheng on 2017/3/21
 */
public class ServerAdapter extends BaseAdapter {


    public ServerAdapter(ArrayList<View> viewList,
            ArrayList<TaskBean> dataList, Context context)
    {
        this.viewList = viewList;
        mDataList = dataList;
        mContext = context;
    }

    private ArrayList<View>     viewList;
    private ArrayList<TaskBean> mDataList;
    private Context             mContext;



    class ViewHolder {
        public View        rootView;
        public TextView    item_tv_ip;
        public TextView    item_tv_wifi_mac;
        public TextView    item_tv_file_name;
        public ProgressBar item_pb;
        public TextView item_tv_file_len;
        public ViewHolder(View rootView) {
            this.rootView = rootView;
            this.item_tv_ip = (TextView) rootView
                    .findViewById(R.id.item_tv_ip);
            this.item_tv_wifi_mac = (TextView) rootView
                    .findViewById(R.id.item_tv_wifi_mac);
            this.item_tv_file_name = (TextView) rootView
                    .findViewById(R.id.item_tv_file_name);
            this.item_tv_file_len = (TextView) rootView
                    .findViewById(R.id.item_tv_file_len);
            this.item_pb = (ProgressBar) rootView
                    .findViewById(R.id.item_pb);
        }

    }

    @Override
    public int getCount() {
        if (mDataList != null) {
            return mDataList.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return mDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = View.inflate(mContext,
                                       R.layout.item_list, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // 设置数据
        TaskBean taskBean = mDataList.get(position);
        convertView.setTag(taskBean.hashCode(),position);
        viewList.add(convertView);
        holder.item_tv_file_name.setText(taskBean.fileName);
        holder.item_tv_ip.setText(taskBean.ip);
        holder.item_tv_wifi_mac.setText(taskBean.wifiMac);
        int size =taskBean.fileLen/1024;
        if (size==0)size+=1;
        holder.item_tv_file_len.setText(size+"kb");
        holder.item_pb.setMax(taskBean.fileLen);
        return convertView;
    }
}

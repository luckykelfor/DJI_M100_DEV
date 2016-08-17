package com.whu.m100;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends BaseAdapter {
    private int selectItem;
    private Context context;
    private List<AprilTag> listItems;

    public HistoryAdapter(Context context) {
        super();

        this.selectItem = -1;
        this.context = context;
        this.listItems = new ArrayList<>();
    }

    public void setSelectItem(int selectItem) {
        this.selectItem = selectItem;
    }

    public void add(AprilTag aprilTag) {
        listItems.add(aprilTag);
    }

    @Override
    public int getCount() {
        return listItems.size();
    }

    @Override
    public Object getItem(int i) {
        return listItems.get(i);
    }

    @Override
    public long getItemId(int id) {
        return id;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        convertView = View.inflate(context, R.layout.list_item, null);

        TextView id = (TextView) convertView.findViewById(R.id.history_id);
        TextView latitude = (TextView) convertView.findViewById(R.id.history_latitude);
        TextView longitude = (TextView) convertView.findViewById(R.id.history_longitude);

        AprilTag aprilTag = listItems.get(i);

        id.setText(aprilTag.getId());
        latitude.setText(aprilTag.getLatitude());
        longitude.setText(aprilTag.getLongitude());

        if (selectItem == i) {
            id.setTextColor(Color.RED);
            id.getPaint().setFakeBoldText(true);

            latitude.setTextColor(Color.RED);
            latitude.getPaint().setFakeBoldText(true);

            longitude.setTextColor(Color.RED);
            longitude.getPaint().setFakeBoldText(true);
        }

        return convertView;
    }
}

package com.radkokotev.glasgow.uni.map;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * An adapter, which wraps the DataHolder class. Used as an adapter for the list view which
 * displays the search results to the user.
 */
public class PlaceAdapter extends BaseAdapter {
    private DataHolder data;
    
    public PlaceAdapter(DataHolder toBeAdded){
        data = toBeAdded;
        
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public EntryHolder getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int index, View view, ViewGroup parent) {
        if (view == null) {        
            // Get a reference to list_view_item.xml (the layout for each individual building item).
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            view = inflater.inflate(R.layout.list_view_item, parent, false);
        }
        
        LinearLayout layout = (LinearLayout)view.findViewById(R.id.listViewLayout);
        layout.setBackgroundColor(Color.argb (220, 0xff, 0xff, 0xff));
        
        EntryHolder building = data.get(index);

        // Populate the building name and ID into text fields for this building entry.
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView idTextView = (TextView) view.findViewById(R.id.id);
        nameTextView.setText(building.getName());
        idTextView.setText("Building " + building.getId() + " on the map");
        
        return view;
    }
}
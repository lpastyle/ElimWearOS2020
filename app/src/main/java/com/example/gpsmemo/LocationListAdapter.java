package com.example.gpsmemo;

import android.content.Context;
import android.location.Location;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.wear.widget.WearableRecyclerView;

import java.util.ArrayList;
import java.util.Date;

public class LocationListAdapter extends WearableRecyclerView.Adapter<LocationListAdapter.LocationViewHolder> {

    // data model
    private ArrayList<Location> locations;

    // UI tools
    public static final String DMYHMS_DATE_FORMAT = "dd/MM/yyyy HH':'mm':'ss";
    private Context context;

    /**
     * Ctor
     *
     * @param list : set of location items to be displayed
     */
    public LocationListAdapter(ArrayList<Location> list, Context context) {
        this.locations = list;
        this.context = context;
    }


    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new item view and return corresponding view holder
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location item = locations.get(position);
        Date date = new Date(item.getTime());
        holder.dateTv.setText(DateFormat.format(DMYHMS_DATE_FORMAT, date).toString());
        String latlon = context.getString(R.string.latlon_format, item.getLatitude(), item.getLongitude());
        holder.latlonTv.setText(latlon);
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    static class LocationViewHolder extends WearableRecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView dateTv;
        TextView latlonTv;

        LocationViewHolder(View v) {
            super(v);
            dateTv = v.findViewById(R.id.date_tv);
            latlonTv = v.findViewById(R.id.lat_lon_tv);
        }
    }


}
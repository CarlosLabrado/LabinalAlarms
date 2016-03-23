package com.ocr.labinal.custom.recyclerView;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ocr.labinal.R;
import com.ocr.labinal.model.PlantEvent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    private static final String TAG = "CustomAdapter";

    private List<PlantEvent> mDataSet;

    Context context;

    // BEGIN_INCLUDE(recyclerViewSampleViewHolder)

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewEventStatus;
        private final TextView textViewEventWorkingOn;
        private final TextView textViewEventBattery;
        private final TextView textViewEventBatteryMin;
        private final TextView textViewTempTime;

        public ViewHolder(View v) {
            super(v);
            textViewEventStatus = (TextView) v.findViewById(R.id.textViewEventStatus);
            textViewEventWorkingOn = (TextView) v.findViewById(R.id.textViewEventWorkingOn);
            textViewEventBattery = (TextView) v.findViewById(R.id.textViewEventBattery);
            textViewEventBatteryMin = (TextView) v.findViewById(R.id.textViewEventBatteryMinutes);
            textViewTempTime = (TextView) v.findViewById(R.id.textViewEventTime);
        }

        public TextView getTextViewEventStatus() {
            return textViewEventStatus;
        }

        public TextView getTextViewEventWorkingOn() {
            return textViewEventWorkingOn;
        }

        public TextView getTextViewEventBattery() {
            return textViewEventBattery;
        }

        public TextView getTextViewEventBatteryMin() {
            return textViewEventBatteryMin;
        }

        public TextView getTextViewTempTime() {
            return textViewTempTime;
        }
    }
    // END_INCLUDE(recyclerViewSampleViewHolder)

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet a list with markers
     */
    public EventAdapter(List<PlantEvent> dataSet, Context newContext) {
        mDataSet = dataSet;
        context = newContext;
    }

    // BEGIN_INCLUDE(recyclerViewOnCreateViewHolder)
    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.event_row_item, viewGroup, false);

        return new ViewHolder(v);
    }
    // END_INCLUDE(recyclerViewOnCreateViewHolder)

    // BEGIN_INCLUDE(recyclerViewOnBindViewHolder)
    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        String statusString = mDataSet.get(position).getState();
        if (statusString.equalsIgnoreCase("alarma")) {
            viewHolder.getTextViewEventStatus().setTextColor(context.getResources().getColor(com.ocr.labinal.R.color.red_600));
        } else {
            viewHolder.getTextViewEventStatus().setTextColor(context.getResources().getColor(R.color.green_700));
        }

        viewHolder.getTextViewEventStatus().setText(mDataSet.get(position).getState());
        viewHolder.getTextViewEventWorkingOn().setText(mDataSet.get(position).getPowerOrigin());
        viewHolder.getTextViewEventBattery().setText(mDataSet.get(position).getUpsState());
        viewHolder.getTextViewEventBatteryMin().setText(String.valueOf(mDataSet.get(position).getMinutesOnBattery()));

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(mDataSet.get(position).getTimeInMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM HH:mm", java.util.Locale.getDefault());
        simpleDateFormat.setCalendar(calendar);

        viewHolder.getTextViewTempTime().setText(simpleDateFormat.format(calendar.getTime()));
    }
    // END_INCLUDE(recyclerViewOnBindViewHolder)

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataSet.size();
    }
}

package com.marcusposey.cadtra;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class RunLogAdapter extends ArrayAdapter<RunLog> {
    private final Context context;
    private final RunLog[] logs;

    public RunLogAdapter(Context context, RunLog[] logs) {
        super(context, -1, logs);
        this.context = context;
        this.logs = logs;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        // Make recent runs appear at the top instead of at the bottom.
        pos = logs.length - 1 - pos;
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.history_item, parent, false);

        ((TextView) row.findViewById(R.id.historyDistance)).setText(logs[pos].getDistance());
        ((TextView) row.findViewById(R.id.historyTime)).setText(logs[pos].getTime());
        ((TextView) row.findViewById(R.id.historyPace)).setText(logs[pos].getPace());

        return row;
    }
}

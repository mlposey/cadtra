package com.marcusposey.cadtra.model;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

/** API representation of a run log, ready to be encoded as JSON */
public class RunLog {
    @SerializedName("started-at")
    private String startTimestampTz;
    @SerializedName("ended-at")
    private String endTimestampTz;
    @SerializedName("polyline")
    private String polylinePath;
    @SerializedName("distance")
    private double distance;
    @SerializedName("split-interval")
    private double splitInterval;
    @SerializedName("splits")
    private double[] splits;
    @SerializedName("comment")
    private String comment;

    public RunLog(String startTimestampTz, String endTimestampTz, String polylinePath,
                  double distance, double splitInterval, double[] splits, String comment) {
        this.startTimestampTz = startTimestampTz;
        this.endTimestampTz = endTimestampTz;
        this.polylinePath = polylinePath;
        this.distance = distance;
        this.splitInterval = splitInterval;
        this.splits = splits;
        this.comment = comment;
    }

    /**
     * Gets the duration of the run in seconds
     * Ideally, this logic would be done once in the constructor and then getter methods use
     * use the final value. However, RunLogs that are created through gson serialization will not
     * invoke the constructor as intended.
     */
    private double getDurationSeconds() {
        // TODO: Make the API specify a duration JSON property.
        // Subtracting start from end does not factor in breaks the user could have taken. This
        // will display an inaccurate average pace if the run was not completed in one go.

        final DateTime start = DateTime.parse(startTimestampTz);
        final DateTime end = DateTime.parse(endTimestampTz);
        return (end.getMillis() - start.getMillis()) / 1000;
    }

    public String getDistance() {
        return String.format("%.2f", distance);
    }

    public String getTime() {
        return Stopwatch.convertTime(getDurationSeconds());
    }

    public String getPace() {
        if (distance < 0.01) return "-";
        return Stopwatch.convertTime(getDurationSeconds() / distance);
    }
}

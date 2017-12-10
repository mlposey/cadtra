package com.marcusposey.cadtra.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import org.joda.time.DateTime;

import java.util.List;

/** API representation of a run log, ready to be encoded as JSON */
public class RunLog {
    @SerializedName("started-at")
    private String startTimestampTz;
    @SerializedName("ended-at")
    private String endTimestampTz;
    @SerializedName("duration")
    private double durationSec;
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

    public RunLog(String startTimestampTz, String endTimestampTz, double durationSec,
                  String polylinePath, double distance, double splitInterval, double[] splits,
                  String comment) {
        this.startTimestampTz = startTimestampTz;
        this.endTimestampTz = endTimestampTz;
        this.durationSec = durationSec;
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

    /**
     * Builds a RunLog using a variable number of parameters
     *
     * Methods:
     *  - add*
     *      These store information about a run. All add methods must be called
     *      before any others.
     *  - calculate*
     *      These create new data using information obtained from adds. Call them
     *      after add* and before use*.
     *  - use*
     *      These change the format of data and should be called last but before
     *      invoking the build() method.
     */
    public static class Builder {
        private String startTimestampTz;
        private String endTimestampTz;
        private double durationSec;
        List<LatLng> route;
        private String polylinePath;
        private String comment;
        private double distance;
        private double splitInterval;
        private double[] splits = null;

        public RunLog build() {
            setDefaults();
            return new RunLog(startTimestampTz, endTimestampTz, durationSec, polylinePath,
                    distance, splitInterval, splits, comment);
        }

        private void setDefaults() {
            if (splits == null) splits = new double[0];

            if (durationSec == 0.0) {
                DateTime start = DateTime.parse(startTimestampTz);
                DateTime end = DateTime.parse(endTimestampTz);
                durationSec = (end.getMillis() - start.getMillis()) / 1000;
            }

            if (distance == 0.0) distance = SphericalUtil.computeLength(route) * 0.00062137;
        }

        /**
         * Stores the time span between session start and end
         * If the session was paused, this is not the actual duration since it
         * includes the periods of inactivity.
         */
        public Builder addTimeSegment(String start, String end) {
            startTimestampTz = start;
            endTimestampTz = end;
            return this;
        }

        public Builder addDuration(double durationSec) {
            this.durationSec = durationSec;
            return this;
        }

        public Builder addRoute(List<LatLng> route) {
            this.route = route;
            polylinePath = PolyUtil.encode(route);
            return this;
        }

        public Builder addSplitData(double splitInterval, double[] splits) {
            this.splitInterval = splitInterval;
            this.splits = splits;
            return this;
        }

        public Builder useMeters() {
            distance = SphericalUtil.computeLength(route);
            return this;
        }

        public Builder useMiles() {
            distance = SphericalUtil.computeLength(route) * 0.00062137;
            return this;
        }
    }
}

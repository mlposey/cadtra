package com.marcusposey.cadtra.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.util.List;

/** Details about a workout session, created using a Builder */
public class WorkoutSession {
    private String startTimestampTz;
    private String endTimestampTz;
    private String polylinePath;
    private String comment;
    private double distance;
    private double splitInterval;
    private double[] splits;

    private WorkoutSession(String startTimestampTz, String endTimestampTz, String polylinePath,
                           String comment, double distance, double splitInterval, double[] splits) {
        this.startTimestampTz = startTimestampTz;
        this.endTimestampTz = endTimestampTz;
        this.polylinePath = polylinePath;
        this.comment = comment;
        this.distance = distance;
        this.splitInterval = splitInterval;
        this.splits = splits;
    }

    public String getStartTimestampTz() {
        return startTimestampTz;
    }

    public String getEndTimestampTz() {
        return endTimestampTz;
    }

    public String getPolylinePath() {
        return polylinePath;
    }

    public String getComment() {
        return comment;
    }

    public double getDistance() {
        return distance;
    }

    public double getSplitInterval() {
        return splitInterval;
    }

    public double[] getSplits() {
        return splits;
    }

    /**
     * Builds a WorkoutSession object.
     *
     * Methods of the form add* add data about a workout, and those of the form calculate* use
     * the added data to create new metrics. As such, ensure add calls come before calculate ones.
     */
    public static class Builder {
        private String startTimestampTz;
        private String endTimestampTz;
        List<LatLng> route;
        private String polylinePath;
        private String comment;
        private double distance;
        private double splitInterval;
        private double[] splits = null;

        public WorkoutSession build() {
            // Create appropriate defaults.
            if (splits == null) splits = new double[0];

            return new WorkoutSession(
                    startTimestampTz, endTimestampTz, polylinePath, comment, distance, splitInterval,
                    splits
            );
        }

        public Builder addTimeSegment(String start, String end) {
            startTimestampTz = start;
            endTimestampTz = end;
            return this;
        }

        public Builder addRoute(List<LatLng> route) {
            this.route = route;
            polylinePath = PolyUtil.encode(route);
            return this;
        }

        public Builder addComment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder addSplitData(double splitInterval, double[] splits) {
            this.splitInterval = splitInterval;
            this.splits = splits;
            return this;
        }

        public Builder calculateDistanceMeter() {
            distance = SphericalUtil.computeLength(route);
            return this;
        }

        public Builder calculcateDistanceMiles() {
            distance = SphericalUtil.computeLength(route) * 0.00062137;
            return this;
        }
    }
}

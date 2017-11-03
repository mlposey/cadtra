package com.marcusposey.cadtra;

import java.util.Observable;
import java.util.concurrent.TimeUnit;

/**
 * Keeps track of time and allows consumers to access the duration in string format
 */
public class Stopwatch extends Observable implements Runnable {
    private long elapsedTime; // Total time (in seconds) taken since last reset
    private long startTime;   // Last time start() was called
    private long curTime;
    private boolean isRunning = false;
    private Thread timer;

    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        if (isRunning()) return;
        isRunning = true;

        timer = new Thread(this);
        timer.start();
    }

    public void stop() {
        if (!isRunning()) return;
        isRunning = false;

        timer.interrupt();
        elapsedTime += (curTime - startTime) / 1000;
        timer = null;
    }

    public void reset() {
        stop();
        elapsedTime = startTime = curTime = 0;
        setChanged();
        notifyObservers(convertTime(0));
    }

    /** Seconds elapsed between last start() and stop() call */
    public long elapsedSeconds() {
        if (isRunning()) return elapsedTime + (curTime - startTime) / 1000;
        return elapsedTime;
    }

    /** Converts seconds to an Hh:Mm:Ss string */
    public static String convertTime(double seconds) {
        String hours = String.valueOf(Math.floor(seconds / 3600));
        long minutes = (long) Math.floor((seconds % 3600) / 60);
        long secs = (long) Math.floor(seconds % 60);

        String format = "";
        if (!hours.equals("0.0")) {
            format += hours + ":";
        }
        format += String.format("%02d:%02d", minutes, secs);

        return format;
    }

    @Override
    public void run() {
        startTime = System.currentTimeMillis();
        while (isRunning) {
            curTime = System.currentTimeMillis();
            setChanged();
            notifyObservers(convertTime(elapsedTime + (curTime - startTime) / 1000));
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException e) {e.printStackTrace();}
        }
    }
}

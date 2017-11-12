package com.marcusposey.cadtra;

import java.util.Observable;
import java.util.concurrent.TimeUnit;

/** Keeps track of elapsed time */
public class Stopwatch extends Observable implements Runnable {
    private long elapsedTime; // Total time (in seconds) taken since last reset
    private long startTime;   // Last time (in milliseconds) start() was called
    private long curTime;     // The current time (in milliseconds)

    // True if the stopwatch is currently measuring time; false otherwise
    private boolean isRunning = false;

    // The thread where an ongoing measurement is taking place
    private Thread timer;

    /** Returns true if the stopwatch is running; false otherwise */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Starts the stopwatch
     *
     * If it was already running, nothing will happen. Consider checking
     * the result of isRunning() if the state is not known.
     */
    public void start() {
        if (isRunning()) return;
        isRunning = true;

        timer = new Thread(this);
        timer.start();
    }

    /**
     * Stops the stopwatch
     *
     * If it was not running, nothing will happen. Consider checking
     * the result of isRunning() if the state is not known.
     */
    public void stop() {
        if (!isRunning()) return;
        isRunning = false;

        timer.interrupt();
        elapsedTime += (curTime - startTime) / 1000;
        timer = null;
    }

    /** Resets the stopwatch, clearing all times */
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

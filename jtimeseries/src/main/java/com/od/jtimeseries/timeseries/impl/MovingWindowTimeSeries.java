/**
 * Copyright (C) 2011 (nick @ objectdefinitions.com)
 *
 * This file is part of JTimeseries.
 *
 * JTimeseries is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JTimeseries is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JTimeseries.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.od.jtimeseries.timeseries.impl;

import com.od.jtimeseries.timeseries.*;
import com.od.jtimeseries.timeseries.util.SeriesUtils;
import com.od.jtimeseries.util.NamedExecutors;
import com.od.jtimeseries.util.time.FixedTimeSource;
import com.od.jtimeseries.util.time.TimePeriod;
import com.od.jtimeseries.util.time.TimeSource;

import javax.swing.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by IntelliJ IDEA.
 * User: Nick
 * Date: 30/01/11
 * Time: 19:15
 *
 * A TimeSeries which exposes to view only those TimeSeriesItem which fall within an adjustable window of time
 *
 * Although the series will store items with timestamps which fall outside the view period, these are not visible
 * unless the start and end times of the window are adjusted in such a way that they will be included in the view.
 *
 * This behaviour requires some of the usual contract of TimeSeries interface to be broken -
 * In particular, the methods to add items do no necessarily result in those items being visible via iteration or
 * get() methods, for example, and adding or removing an item may not affect the size(), unless that item falls within
 * the current time window.
 *
 * The start and end time can be specified as a TimeSource, rather than absolute time value, and the MovingWindowTimeSeries
 * can be set to periodically adjust the window based on the new timestamp values supplied by the TimeSource for each end.
 * This means it is easy, for example, to define a window which shows only the most recent 10 minutes worth of data,
 * the window being dynamically adjusted as time moves inexorably forward.
 */
public class MovingWindowTimeSeries extends AbstractIndexedTimeSeries {

    private static ScheduledExecutorService scheduledExecutorService = NamedExecutors.newScheduledThreadPool(MovingWindowTimeSeries.class.getSimpleName(), 2);

    public static final TimeSource OPEN_END_TIME = new FixedTimeSource(Long.MAX_VALUE); //ending at the end of the epoch
    public static final TimeSource OPEN_START_TIME = new FixedTimeSource(0);  //starting at the start of the epoch

    //use this privately owned series to store the items in the full series
    //this MovingWindowTimeSeries provides a movable window onto the items on this wrapped series
    private DefaultTimeSeries wrappedTimeSeries = new DefaultTimeSeries();

    private long startTime;
    private int startIndex = -1;
    private TimeSource startTimeSource;
    private long endTime;
    private int endIndex = -1;
    private TimeSource endTimeSource;
    private final AtomicLong modCount = new AtomicLong(0);
    private ScheduledFuture windowCheckFuture;
    private boolean updateWindowInSwingEventThread;

    public MovingWindowTimeSeries() {
        this(OPEN_START_TIME, OPEN_END_TIME, null);
    }

    public MovingWindowTimeSeries(TimeSource startTimeSource, TimeSource endTimeSource, TimePeriod frequencyToCheckWindow) {
        this.startTimeSource = startTimeSource;
        this.endTimeSource = endTimeSource;
        findStartAndEndAndFireChange();
        if ( frequencyToCheckWindow != null) {
            UpdateWindowTask task = new UpdateWindowTask(this);
            windowCheckFuture = scheduledExecutorService.scheduleWithFixedDelay(
                task,
                frequencyToCheckWindow.getLengthInMillis(),
                frequencyToCheckWindow.getLengthInMillis(),
                TimeUnit.MILLISECONDS
            );
            task.setFuture(windowCheckFuture);
        }
    }

    //our start and end times are changing, we need to recalculate the window start and end index.
    //If this affects the items which should appear in the window, fire an event
    private void findStartAndEndAndFireChange() {
        Runnable runnable = new Runnable() {
            public void run() {
                synchronized (MovingWindowTimeSeries.this) {
                    int oldStartIndex = startIndex;
                    int oldEndIndex = endIndex;

                    startTime = startTimeSource.getTime();
                    endTime = endTimeSource.getTime();
                    startIndex = SeriesUtils.getIndexOfFirstItemAtOrAfter(startTime, wrappedTimeSeries);
                    endIndex = SeriesUtils.getIndexOfFirstItemAtOrBefore(endTime, wrappedTimeSeries);

                    if ( startIndex == -1 || endIndex == -1) {
                        startIndex = endIndex = -1;
                    }

                    if ( oldStartIndex != startIndex || oldEndIndex != endIndex ) {
                        long newModCount = modCount.incrementAndGet();
                        queueSeriesChangedEvent(TimeSeriesEvent.createSeriesChangedEvent(MovingWindowTimeSeries.this, getSnapshot(), newModCount ));
                    }
                }
            }
        };

        if ( updateWindowInSwingEventThread && ! SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }  else {
            runnable.run();
        }
    }

    /**
     * Run tasks to update the window in the swing even thread
     * If only the event thread is used to call add/remove or otherwise modify the wrapped series, this will ensure
     * consistency of the model as it appears to the Swing event thread
     */
    public void setUpdateWindowInSwingEventThread(boolean updateWindowInSwingEventThread) {
        this.updateWindowInSwingEventThread = updateWindowInSwingEventThread;
    }

    public synchronized int size() {
        return startIndex >= 0 && endIndex >= 0 ? (endIndex - startIndex) + 1 : 0;
    }

    public synchronized Iterator<TimeSeriesItem> iterator() {
        return new WindowIterator();
    }

    public synchronized void addItem(TimeSeriesItem timeSeriesItem) {
        wrappedTimeSeries.addItem(timeSeriesItem);
        if (isInWindow(timeSeriesItem)) {

            if ( endIndex == -1) {
                endIndex = SeriesUtils.getIndexOfFirstItemAtOrBefore(timeSeriesItem.getTimestamp(), wrappedTimeSeries);
            } else {
                endIndex ++;
            }
            if ( startIndex == -1 ) {
                startIndex = endIndex;
            }

            queueItemsAddedOrInsertedEvent(
                TimeSeriesEvent.createItemsAddedOrInsertedEvent(
                    this,
                    Collections.singletonList(timeSeriesItem),
                    modCount.incrementAndGet()
                )
            );
        }
    }

    public synchronized boolean removeItem(TimeSeriesItem timeSeriesItem) {
        boolean removed = wrappedTimeSeries.removeItem(timeSeriesItem);
        if ( removed && isInWindow(timeSeriesItem)) {
            endIndex--;
            if ( endIndex == -1 ) {
                startIndex = endIndex;
            }

            queueItemsRemovedEvent(
                TimeSeriesEvent.createItemsRemovedEvent(
                    this,
                    Collections.singletonList(timeSeriesItem),
                    modCount.incrementAndGet()
                )
            );
        }
        return removed;
    }

    public synchronized void clear() {
        wrappedTimeSeries.clear();
        queueSeriesChangedEvent(
            TimeSeriesEvent.createSeriesChangedEvent(
                this,
                Collections.<TimeSeriesItem>emptyList(),
                modCount.incrementAndGet()
            )
        );
    }

    public synchronized TimeSeriesItem getItem(int index) {
        if (index >= -1 && index < size() ) {
            return wrappedTimeSeries.getItem(getRealIndex(index));
        } else {
            throw new IndexOutOfBoundsException("Index " + index + " must be between 0 and " + (size() - 1));
        }
    }

    public synchronized TimeSeriesItem getEarliestItem() {
        return size() == 0 ? null : wrappedTimeSeries.getItem(startIndex);
    }

    public synchronized TimeSeriesItem getLatestItem() {
        return size() == 0 ? null : wrappedTimeSeries.getItem(endIndex);
    }

    public synchronized void addTimeSeriesListener(TimeSeriesListener l) {
        //we manage our own listeners and events, don't delegate this
        //to the wrapped series
        super.addTimeSeriesListener(l);
    }

    public synchronized void removeTimeSeriesListener(TimeSeriesListener l) {
        //we manage our own listeners and events, don't delegate this
        //to the wrapped series
        super.removeTimeSeriesListener(l);
    }

    //the apparent modCount of the window/view
    public synchronized long getModCount() {
        return modCount.get();
    }

    public synchronized TimeSource getEndTime() {
        return endTimeSource;
    }

    public synchronized void setEndTime(TimeSource endTimeSource) {
        this.endTimeSource = endTimeSource;
        findStartAndEndAndFireChange();
    }

    public synchronized TimeSource getStartTime() {
        return startTimeSource;
    }

    public synchronized void setStartTime(TimeSource startTimeSource) {
        this.startTimeSource = startTimeSource;
        findStartAndEndAndFireChange();
    }

    public synchronized void setStartTime(long startTime) {
        setStartTime(new FixedTimeSource(startTime));
    }

    public synchronized void setEndTime(long endTime) {
        setEndTime(new FixedTimeSource(endTime));
    }

    public synchronized List<TimeSeriesItem> getSnapshot() {
        return getItemsInRange(startTime, endTime);
    }

    /**
     * Changing this data in any way may corrupt the windowed view
     * @return the underlying series data for this windowed time series
     */
    public TimeSeries getWrappedSeries() {
        return wrappedTimeSeries;
    }

    private boolean isInWindow(int realIndex) {
        return realIndex >= startIndex && realIndex <= endIndex;
    }

    public synchronized boolean isInWindow(TimeSeriesItem item) {
        long timeStamp = item.getTimestamp();
        return timeStamp >= startTime && timeStamp <= endTime;
    }

    private int getRealIndex(int viewIndex) {
        return viewIndex + startIndex;
    }

    //an iterator backed by the wrapped series, which exposes only the items currently in the window
    private class WindowIterator implements Iterator<TimeSeriesItem> {

        private int index = -1;
        private long lastModCount;

        public WindowIterator() {
            lastModCount = getModCount();
        }

        public boolean hasNext() {
            lastModCount = getModCount();
            return size() > index + 1;
        }

        public TimeSeriesItem next() {
            if ( lastModCount != getModCount()) {
                throw new ConcurrentModificationException();
            }
            if ( ! hasNext()) {
                throw new NoSuchElementException("No next element");
            }
            return getItem(++index);
        }

        public void remove() {
            if ( lastModCount != getModCount()) {
                throw new ConcurrentModificationException();
            }

            synchronized (MovingWindowTimeSeries.this) {
                removeItem(getItem(index));
                lastModCount = getModCount();
            }
            index--;
        }
    }

    private static class UpdateWindowTask implements Runnable {

        private WeakReference<MovingWindowTimeSeries> series;
        private volatile ScheduledFuture future;

        public UpdateWindowTask(MovingWindowTimeSeries s) {
            this.series = new WeakReference<MovingWindowTimeSeries>(s);
        }

        public void setFuture(ScheduledFuture future) {
            this.future = future;
        }

        public void run() {
            final MovingWindowTimeSeries s = series.get();
            if ( s != null ) {
               s.findStartAndEndAndFireChange();
            } else {
                if ( future != null ) {
                    future.cancel(false);
                } else {
                    //throwing an exception should also cancel the task
                    throw new RuntimeException("Cannot find future to cancel task");
                }
            }
        }
    }
}

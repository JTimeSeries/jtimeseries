package com.od.jtimeseries.server.timeseries;

import com.od.jtimeseries.source.Counter;
import com.od.jtimeseries.source.ValueRecorder;
import com.od.jtimeseries.source.impl.DefaultCounter;
import com.od.jtimeseries.source.impl.DefaultValueRecorder;
import com.od.jtimeseries.util.NamedExecutors;
import com.od.jtimeseries.util.logging.LogMethods;
import com.od.jtimeseries.util.logging.LogUtils;
import com.od.jtimeseries.util.time.Time;
import com.od.jtimeseries.util.time.TimePeriod;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 03/04/12
 * Time: 09:10
 *
 * Hold references to timeseries in a strong referenced LRU cache which may increase in size until
 * a configurable percentage of available memory is used
 */
public class TimeSeriesMapCache<K,E> implements TimeSeriesCache<K,E> {

    private static LogMethods logMethods = LogUtils.getLogMethods(TimeSeriesMapCache.class);

    private Counter cacheRequests = DefaultCounter.NULL_COUNTER;
    private long lastRequestCount;
    private AtomicLong cacheHits = new AtomicLong();
    private Counter cacheSizeCounter = DefaultCounter.NULL_COUNTER;
    private Counter cacheItemCount = DefaultCounter.NULL_COUNTER;
    private Counter cacheRemoves = DefaultCounter.NULL_COUNTER;
    private ValueRecorder cacheHitPercentage = DefaultValueRecorder.NULL_VALUE_RECORDER;

    /**
     * Initial max size of cache
     */
    private int DEFAULT_INITIAL_SIZE = 128;

    /**
     * percentage by which to increase or decrease cache size when the cache is expanded / shrunk
     */
    private final int increaseDecreasePercent;

    /**
     * percentage of max memory at which cache size will be reduced
     */
    public final double cacheShrinkThresholdPercent;

    /**
     * memory usage percentage after which cache expansions are denied
     */
    public final int maxMemoryForCacheExpansionPercent;


    private volatile int maxSize = DEFAULT_INITIAL_SIZE;

    /**
     * Minimum intervals between cache size increases
     */
    private TimePeriod minimumExpansionInterval = Time.milliseconds(1000);


    private long lastSizeCheck;


    private final LinkedHashMap<K,E> cache = new LinkedHashMap<K,E>(DEFAULT_INITIAL_SIZE, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            boolean result = size() > maxSize;
            if ( result ) {
                cacheRemoves.incrementCount();
            }
            return result;
        }
    };

    private ScheduledExecutorService cacheExecutorService = NamedExecutors.newSingleThreadScheduledExecutor(getClass().getSimpleName());

    public TimeSeriesMapCache() {
        this(256, 20, 60, Time.seconds(10), 90);
    }

    public TimeSeriesMapCache(int maxInitialSize) {
        this(maxInitialSize, 20, 60, Time.seconds(10), 90);
    }

    public TimeSeriesMapCache(int maxInitialSize, int increaseDecreasePercent, int maxMemoryForCacheExpansionPercent, TimePeriod minimumExpansionInterval, double cacheShrinkThresholdPercent) {
        this.maxSize = maxInitialSize;
        this.increaseDecreasePercent = increaseDecreasePercent;
        this.maxMemoryForCacheExpansionPercent = maxMemoryForCacheExpansionPercent;
        this.minimumExpansionInterval = minimumExpansionInterval;
        this.cacheShrinkThresholdPercent = cacheShrinkThresholdPercent;
        scheduleShrinkCacheTask();
        scheduleCacheHitPercentageCalculation();
    }

    private void scheduleCacheHitPercentageCalculation() {
        cacheExecutorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                long currentCount = cacheRequests.getCount();
                double cacheHitPercent = ((double) cacheHits.getAndSet(0)) / (currentCount - lastRequestCount) * 100;
                lastRequestCount = currentCount;
                cacheHitPercentage.newValue(cacheHitPercent);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    private void scheduleShrinkCacheTask() {
        cacheExecutorService.scheduleWithFixedDelay(new DecreaseCacheSizeTask(), 60, 60, TimeUnit.SECONDS);
    }

    public E get(K key) {
        synchronized(cache)  {
            cacheRequests.incrementCount();

            E result = cache.get(key);
            if ( result != null) {
                cacheHits.incrementAndGet();
            }
            return result;
        }
    }

    public E put(K key, E value) {
        synchronized(cache) {
            //since we are using LinkedHashMap in LRU cache mode, this may also cause oldest item to be dropped from map
            E result = cache.put(key, value);

            cacheItemCount.setCount(cache.size());
            if ( cache.size() == maxSize) {
                scheduleCacheSizeIncrease();
            }
            return result;
        }
    }

    public void remove(K key) {
        synchronized(cache) {
            cache.remove(key);
        }
    }

    public void setCacheSizeCounter(Counter cacheSizeCounter) {
        cacheSizeCounter.setCount(maxSize);
        this.cacheSizeCounter = cacheSizeCounter;
    }

    public void setCacheSeriesCounter(Counter cacheItemCount) {
        this.cacheItemCount = cacheItemCount;
    }

    public void setCacheRemovesCounter(Counter cacheRemoves) {
        this.cacheRemoves = cacheRemoves;
    }

    public void setCacheHitPercentage(ValueRecorder cacheHitPercentage) {
        this.cacheHitPercentage = cacheHitPercentage;
    }

    public void setCacheRequestCounter(Counter cacheRequestCounter) {
        this.cacheRequests = cacheRequestCounter;
    }

    private void scheduleCacheSizeIncrease() {
        if ( System.currentTimeMillis() - lastSizeCheck > minimumExpansionInterval.getLengthInMillis())  {
            logMethods.debug("Cache is full, checking whether a size increase is possible");
            lastSizeCheck = System.currentTimeMillis();
            IncreaseSizeCheckTask t = new IncreaseSizeCheckTask();
            cacheExecutorService.execute(t);
        }
    }

    private class IncreaseSizeCheckTask implements Runnable {

        public void run() {
            synchronized(cache) {
                //while we are using less than half the max memory
                //allow the cache size to keep increasing
                double utilisationPercent = getMemoryUtilisationPercent();

                if ( utilisationPercent < maxMemoryForCacheExpansionPercent) {
                    maxSize *= ( 100 + increaseDecreasePercent) / 100f;
                    logMethods.info("Used memory " + utilisationPercent + " percent, max for increase " +
                            maxMemoryForCacheExpansionPercent + ", will increase cache size to " + maxSize);
                    cacheSizeCounter.setCount(maxSize);
                }
            }
        }
    }

    private class DecreaseCacheSizeTask implements Runnable {

        public void run() {
            synchronized(cache) {
                double utilisationPercent = getMemoryUtilisationPercent();
                if ( utilisationPercent > cacheShrinkThresholdPercent) {
                    maxSize *= ( 100 - increaseDecreasePercent) / 100f;
                    cacheSizeCounter.setCount(maxSize);
                    logMethods.info("Used memory " + utilisationPercent + " percent, will decrease cache size by " +
                            increaseDecreasePercent + " percent to " + maxSize);
                    int toRemove = cache.size() - maxSize;
                    while ( toRemove > 0) {
                        removeFromCacheAndResetUsageCounts(toRemove, false);
                        toRemove = cache.size() - maxSize;
                    }
                }
            }
        }

        private void removeFromCacheAndResetUsageCounts(int toRemove, boolean b) {
            Iterator i = cache.entrySet().iterator();
            int removed = 0;
            while ( i.hasNext() && removed < toRemove) {
                i.next();
                i.remove();
                removed++;
            }
        }
    }

    private double getMemoryUtilisationPercent() {
        double maxAvailable = Runtime.getRuntime().maxMemory();
        double total = Runtime.getRuntime().totalMemory();
        double free = Runtime.getRuntime().freeMemory();

        double utilisedMemory = total - free;
        double utilisationRatio = utilisedMemory / maxAvailable;
        return utilisationRatio * 100;
    }

}

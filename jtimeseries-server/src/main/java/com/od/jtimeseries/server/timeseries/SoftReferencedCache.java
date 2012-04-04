package com.od.jtimeseries.server.timeseries;

import com.od.jtimeseries.source.Counter;
import com.od.jtimeseries.source.ValueRecorder;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 04/04/12
 * Time: 09:31
 *
 * A TimeSeriesCache which uses soft references to hold on to cached instances
 */
public class SoftReferencedCache<K,V> implements TimeSeriesCache<K,V> {

    private ConcurrentHashMap<K, SoftReference<V>> cache = new ConcurrentHashMap<K, SoftReference<V>>();


    public V get(K key) {
        SoftReference<V> s = cache.get(key);
        return s == null ? null : s.get();
    }

    public V put(K key, V value) {
        SoftReference<V> old = cache.put(key, new SoftReference<V>(value));
        return old == null ? null : old.get();
    }

    public void remove(K key) {
        cache.remove(key);
    }

    //metrics not yet supported for soft referenced cache
    public void setCacheSizeCounter(Counter cacheSizeCounter) {
    }

    public void setCacheSeriesCounter(Counter cacheItemCount) {
    }

    public void setCacheRemovesCounter(Counter cacheRemoves) {
    }

    public void setCacheHitPercentage(ValueRecorder cacheHitPercentage) {
    }

    public void setCacheRequestCounter(Counter c) {
    }
}
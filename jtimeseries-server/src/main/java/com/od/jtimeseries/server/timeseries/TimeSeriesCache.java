package com.od.jtimeseries.server.timeseries;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 04/04/12
 * Time: 09:25
 *
 * Cache for timeseries instances
 */
public interface TimeSeriesCache<K, E> {

    /**
     * @return an item, if it exists in the cache
     */
    E get(K key);

    /**
     * add an item to the cache, if there is sufficient capacity
     */
    E put(K key, E value);


    void remove(K key);
}

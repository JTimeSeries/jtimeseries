/**
 * Copyright (C) 2009 (nick @ objectdefinitions.com)
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
package com.od.jtimeseries.context;

import com.od.jtimeseries.capture.Capture;
import com.od.jtimeseries.capture.CaptureFactory;
import com.od.jtimeseries.capture.TimedCapture;
import com.od.jtimeseries.capture.function.CaptureFunction;
import com.od.jtimeseries.scheduling.Scheduler;
import com.od.jtimeseries.source.*;
import com.od.jtimeseries.timeseries.IdentifiableTimeSeries;
import com.od.jtimeseries.timeseries.TimeSeriesFactory;
import com.od.jtimeseries.util.identifiable.Identifiable;
import com.od.jtimeseries.util.time.TimePeriod;
import com.od.jtimeseries.context.impl.ContextMetricCreator;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 17-Dec-2008
 * Time: 17:25:10
 *
 * A context for storing and creating timeseries
 *
 * TimeSeriesContext has several convenience methods such as createValueRecorder() which allow the user to create
 * a value source, a capture and a time series within the context in one method call.
 *
 * An example is the easiest way to illustrate this:
 *
 * <pre>
 * e.g.
 * ValueRecorder vr = context.newValueRecorder("Memory", "Memory Usage", CaptureFunctions.MEAN(Time.mins(5));
 * vr.newValue(10);
 * vr.newValue(20);
 * ...
 * </pre>
 *
 * The above would create a ValueRecorder which can be used to store values, a TimedCapture to aggregate
 * the values every five minutes using the MEAN function, and TimeSeries which stores the mean values from the function.
 * If you wish to store raw values from the valueRecorder, without performing any aggregation, do not specify a CaptureFunction,
 * or use the special function CaptureFunctions.RAW_VALUES
 */
public interface TimeSeriesContext extends Identifiable, ContextQueries {

    <E extends Identifiable> E create(String id, String description, Class<E> clazz);

    TimeSeriesContext getParent();

    TimeSeriesContext getRoot();

    List<ValueSource> getSources();

    List<Capture> getCaptures();

    List<TimeSeriesContext> getChildContexts();

    List<IdentifiableTimeSeries> getTimeSeries();

    TimeSeriesContext addChild(Identifiable... identifiables);

    IdentifiableTimeSeries getTimeSeries(String path);

    ValueSource getSource(String id);

    TimeSeriesContext getContext(String path);

    Capture getCapture(String id);

    Scheduler getScheduler();

    TimeSeriesContext setScheduler(Scheduler scheduler);

    boolean isSchedulerStarted();

    TimeSeriesContext startScheduling();

    TimeSeriesContext stopScheduling();

    TimeSeriesContext startDataCapture();

    TimeSeriesContext stopDataCapture();
    
    TimeSeriesContext setValueSourceFactory(ValueSourceFactory sourceFactory);

    ValueSourceFactory getValueSourceFactory();

    TimeSeriesContext setTimeSeriesFactory(TimeSeriesFactory seriesFactory);

    TimeSeriesFactory getTimeSeriesFactory();

    TimeSeriesContext setCaptureFactory(CaptureFactory captureFactory);

    CaptureFactory getCaptureFactory();

    TimeSeriesContext setContextFactory(ContextFactory contextFactory);

    ContextFactory getContextFactory();

    /**
     * Create a child context with the given id, which will also be used for the description
     */
    TimeSeriesContext createContext(String path);

    /**
     * Create a child context with the given id and description
     */
    TimeSeriesContext createContext(String path, String description);

    /**
     * Create a new IdentifiableTimeSeries and add it to this context
     */
    IdentifiableTimeSeries createTimeSeries(String path, String description);

    /**
     * Create a new Capture to capture values from source into series, and add it to this context
     */
    Capture createCapture(String id, ValueSource source, IdentifiableTimeSeries series);

    /**
     * Create a new TimedCapture to periodically capture values from source into series using the captureFunction, and add it to this context
     */
    TimedCapture createTimedCapture(String id, ValueSource source, IdentifiableTimeSeries series, CaptureFunction captureFunction);

    /**
     * Create a ValueRecorder and add it to this context, without creating an associated Capture and TimeSeries
     */
    ValueRecorder createValueRecorder(String id, String description);

    /**
     * Create a QueueTimer and add it to this context, without creating an associated Capture and TimeSeries
     */
    QueueTimer createQueueTimer(String id, String description);

    /**
     * Create a Counter and add it to this context, without creating an associated Capture and TimeSeries
     */
    Counter createCounter(String id, String description);

    /**
     * Create a EventTimer and add it to this context, without creating an associated Capture and TimeSeries
     */
    EventTimer createEventTimer(String id, String description);

    /**
     * Create a TimedValueSource and add it to this context, without creating an associated Capture and TimeSeries
     */
    TimedValueSource createTimedValueSource(String id, String description, ValueSupplier valueSupplier, TimePeriod timePeriod);

    /**
     * Create a valueRecorder, capture(s) and timeseries within this context
     * The timeseries will be created they if do not yet exist, otherwise the valueRecorder will be bound to the existing series
     *
     * If no captureFunctions are specified, a single a capture and timeseries will be created to store the raw values from the
     * valueRecorder. The timeseries will have the id and description provided.
     *
     * Alternatively, if captureFunction(s) are specified, a TimedCapture and timeseries will be created for each function.
     * The TimedCapture uses the function to aggregate the values received from the valueRecorder, and stores the aggregate value
     * into a timeseries periodically. (For example, the median value every 5 minutes).
     * In this case, the id of the timeseries created will be derived from the supplied id and the choice of CaptureFunction
     *
     * @param id, id for the time series to be created
     * @param description, description of the time series
     * @param captureFunctions, functions to aggregate values across a time period
     * @return new valueRecorder instance
     */
    ValueRecorder createValueRecorderSeries(String id, String description, CaptureFunction... captureFunctions);

    /**
     * Create a queueTimer, capture(s) and timeseries within this context
     * The timeseries will be created they if do not yet exist, otherwise the queueTimer will be bound to the existing series
     *
     * If no captureFunctions are specified, a single a capture and timeseries will be created to store the raw values from the
     * queueTimer. The timeseries will have the id and description provided.
     *
     * Alternatively, if captureFunction(s) are specified, a TimedCapture and timeseries will be created for each function.
     * The TimedCapture uses the function to aggregate the values received from the queueTimer, and stores the aggregate value
     * into a timeseries periodically. (For example, the median value every 5 minutes).
     * In this case, the id of the timeseries created will be derived from the supplied id and the choice of CaptureFunction
     *
     * @param id, base id for the time series to be created
     * @param description, description of the time series
     * @param captureFunctions, functions to aggregate values across a time period
     * @return new queueTimer instance
     */
    QueueTimer createQueueTimerSeries(String id, String description, CaptureFunction... captureFunctions);

    /**
     * Create a counter, capture(s) and timeseries within this context
     * The timeseries will be created they if do not yet exist, otherwise the counter will be bound to the existing series
     *
     * If no captureFunctions are specified, a single a capture and timeseries will be created to store the raw values from the
     * counter. The timeseries will have the id and description provided.
     *
     * Alternatively, if captureFunction(s) are specified, a TimedCapture and timeseries will be created for each function.
     * The TimedCapture uses the function to aggregate the values received from the counter, and stores the aggregate value
     * into a timeseries periodically. (For example, the median value every 5 minutes).
     * In this case, the id of the timeseries created will be derived from the supplied id and the choice of CaptureFunction
     *
     * @param id, base id for the time series to be created
     * @param description, description of the time series
     * @param captureFunctions, functions to aggregate values across a time period
     * @return new counter instance
     */
    Counter createCounterSeries(String id, String description, CaptureFunction... captureFunctions);

    /**
     * Create a eventTimer, capture(s) and timeseries within this context
     * The timeseries will be created they if do not yet exist, otherwise the eventTimer will be bound to the existing series
     *
     * If no captureFunctions are specified, a single a capture and timeseries will be created to store the raw values from the
     * eventTimer. The timeseries will have the id and description provided.
     *
     * Alternatively, if captureFunction(s) are specified, a TimedCapture and timeseries will be created for each function.
     * The TimedCapture uses the function to aggregate the values received from the eventTimer, and stores the aggregate value
     * into a timeseries periodically. (For example, the median value every 5 minutes).
     * In this case, the id of the timeseries created will be derived from the supplied id and the choice of CaptureFunction
     *
     * @param id, base id for the time series to be created
     * @param description, description of the time series
     * @param captureFunctions, functions to aggregate values across a time period
     * @return new eventTimer instance
     */
    EventTimer createEventTimerSeries(String id, String description, CaptureFunction... captureFunctions);

    /**
     * Create a timedValueSource, capture(s) and timeseries within this context
     * The timeseries will be created they if do not yet exist, otherwise the timedValueSource will be bound to the existing series
     *
     * The timedValueSource periodically calls getValue() to obtain a value from the valueSupplier provided, and stores the value
     * into a timeseries with the id and description provided
     *
     * @param id, id for the timeseries to be created
     * @param description, description of the time series
     * @return new timedValueSource instance
     */
    TimedValueSource createValueSupplierSeries(String id, String description, ValueSupplier valueSupplier, TimePeriod timePeriod);
}

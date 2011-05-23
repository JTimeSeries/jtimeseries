package com.od.jtimeseries.ui.util;

import com.od.jtimeseries.JTimeSeries;
import com.od.jtimeseries.capture.function.CaptureFunctions;
import com.od.jtimeseries.component.jmx.JmxManagementService;
import com.od.jtimeseries.component.managedmetric.DefaultMetricSource;
import com.od.jtimeseries.component.managedmetric.ManagedMetric;
import com.od.jtimeseries.component.managedmetric.ManagedMetricInitializer;
import com.od.jtimeseries.component.managedmetric.ManagedMetricSource;
import com.od.jtimeseries.component.managedmetric.jmx.DefaultJmxExecutorService;
import com.od.jtimeseries.component.managedmetric.jmx.JmxMetric;
import com.od.jtimeseries.component.managedmetric.jmx.measurement.JmxMeasurement;
import com.od.jtimeseries.component.managedmetric.jmx.measurement.JmxMeasurements;
import com.od.jtimeseries.context.TimeSeriesContext;
import com.od.jtimeseries.source.Counter;
import com.od.jtimeseries.source.ValueRecorder;
import com.od.jtimeseries.timeseries.impl.DefaultIdentifiableTimeSeries;
import com.od.jtimeseries.timeseries.impl.DefaultTimeSeriesFactory;
import com.od.jtimeseries.timeseries.impl.RoundRobinTimeSeries;
import com.od.jtimeseries.util.identifiable.Identifiable;
import com.od.jtimeseries.util.logging.LogMethods;
import com.od.jtimeseries.util.logging.LogUtils;
import com.od.jtimeseries.util.time.Time;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 10-Dec-2010
 * <p/>
 * To change this template use File | Settings | File Templates.
 */
public class LocalJmxMetrics {

    private static final LocalJmxMetrics singleton = new LocalJmxMetrics();
    private static LogMethods logMethods = LogUtils.getLogMethods(LocalJmxMetrics.class);

    private JmxManagementService jmxService = new JmxManagementService();
    private TimeSeriesContext rootContext = JTimeSeries.createRootContext();

    private String metricRootPath = "timeSerious";
    private ValueRecorder queryTimesRecorder;
    private Counter queryCounter;
    private Counter httpSeriesCount;
    private Counter visualizerCount;

    private LocalJmxMetrics() {
        rootContext.setTimeSeriesFactory(new RoundRobinTimeSeriesFactory());

        queryTimesRecorder = rootContext.createValueRecorderSeries(
                metricRootPath + Identifiable.NAMESPACE_SEPARATOR + "Series Query Time",
                "Time taken by queries to load series data in milliseconds",
                CaptureFunctions.RAW_VALUES(),
                CaptureFunctions.MEAN(Time.minutes(1)),
                CaptureFunctions.MAX(Time.minutes(1))
        );

        queryCounter = rootContext.createCounterSeries(
                metricRootPath + Identifiable.NAMESPACE_SEPARATOR + "Series Query Count",
                "Number of queries executed",
                CaptureFunctions.COUNT(Time.minutes(1))
        );

        httpSeriesCount = rootContext.createCounterSeries(
                metricRootPath + Identifiable.NAMESPACE_SEPARATOR + "RemoteHttpTimeSeries Count",
                "Number of memory resident RemoteHttpTimeSeries",
                CaptureFunctions.LAST(Time.minutes(1))
        );

        visualizerCount = rootContext.createCounterSeries(
                metricRootPath + Identifiable.NAMESPACE_SEPARATOR + "Visualizer Count",
                "Number of memory resident TimeSeriesVisualizer",
                CaptureFunctions.LAST(Time.minutes(1))
        );
    }

    public static LocalJmxMetrics getInstance() {
        return singleton;
    }

    public void startJmxManagementService(int port) {
        logMethods.logInfo("Starting jmx management service");
        jmxService.startJmxManagementService(port);
    }

    public void startLocalMetricCollection() {
        logMethods.logInfo("Starting local metric connection");

        JmxMeasurement cpu = JmxMeasurements.cpuTimePercentage(metricRootPath);
        JmxMeasurement memoryPercentOfMax = JmxMeasurements.heapMemoryPercentageOfMax(metricRootPath);
        JmxMeasurement memoryUsage = JmxMeasurements.heapMemoryUsage(metricRootPath);
        JmxMeasurement gcTimePercentage = JmxMeasurements.gcTimePercentage(metricRootPath);

        ManagedMetric m = new JmxMetric(
            Time.seconds(10),
            jmxService.getServiceUrl(),
            Arrays.asList(cpu, memoryPercentOfMax, memoryUsage, gcTimePercentage)
        );

        DefaultMetricSource metricSource = new DefaultMetricSource(Collections.singletonList(m));

        DefaultJmxExecutorService executorService = new DefaultJmxExecutorService(1, 1000 * 60 * 60 * 24); //when this number is exceeded, service should reconnect in any case

        ManagedMetricInitializer managedMetricInitializer = new ManagedMetricInitializer(
            rootContext,
            Collections.singletonList((ManagedMetricSource)metricSource),
            executorService
        );

        managedMetricInitializer.initializeServerMetrics();

        rootContext.startScheduling(); 

    }

    public Counter getQueryCounter() {
        return queryCounter;
    }

    public ValueRecorder getQueryTimesRecorder() {
        return queryTimesRecorder;
    }

    public TimeSeriesContext getRootContext() {
        return rootContext;
    }

    public Counter getHttpSeriesCount() {
        return httpSeriesCount;
    }

    public Counter getVisualizerCount() {
        return visualizerCount;
    }

    private static class RoundRobinTimeSeriesFactory extends DefaultTimeSeriesFactory {

        private static final int ROUND_ROBIN_TIME_SERIES_MAX_SIZE = 1440;

        public <E extends Identifiable> E createTimeSeries(Identifiable parent, String path, String id, String description, Class<E> classType, Object... parameters) {
            if (classType.isAssignableFrom(DefaultIdentifiableTimeSeries.class)) {
                return (E) new DefaultIdentifiableTimeSeries(id, description, new RoundRobinTimeSeries(ROUND_ROBIN_TIME_SERIES_MAX_SIZE));
            }
            throw new UnsupportedOperationException("Cannot create time series of class " + classType);
        }
    }
}

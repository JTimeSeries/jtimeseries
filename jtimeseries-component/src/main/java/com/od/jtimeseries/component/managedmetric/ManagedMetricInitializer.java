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
package com.od.jtimeseries.component.managedmetric;

import com.od.jtimeseries.component.managedmetric.jmx.JmxConnectionPool;
import com.od.jtimeseries.component.managedmetric.jmx.JmxMetric;
import com.od.jtimeseries.component.util.path.PathMapper;
import com.od.jtimeseries.context.TimeSeriesContext;
import com.od.jtimeseries.util.logging.LogMethods;
import com.od.jtimeseries.util.logging.LogUtils;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 22-Nov-2009
 * Time: 00:38:44
 * To change this template use File | Settings | File Templates.
 */
public class ManagedMetricInitializer {

    private static LogMethods logMethods = LogUtils.getLogMethods(ManagedMetricInitializer.class);

    private TimeSeriesContext rootContext;
    private List<ManagedMetricSource> managedMetricSourceList;
    private JmxConnectionPool jmxExecutorService;
    private PathMapper pathMapper;

    public ManagedMetricInitializer(TimeSeriesContext rootContext, List<ManagedMetricSource> managedMetricSourceList, JmxConnectionPool jmxExecutorService) {
        this(rootContext, managedMetricSourceList, jmxExecutorService, new PathMapper());
    }

    public ManagedMetricInitializer(TimeSeriesContext rootContext, List<ManagedMetricSource> managedMetricSourceList, JmxConnectionPool jmxExecutorService, PathMapper pathMapper) {
        this.rootContext = rootContext;
        this.managedMetricSourceList = managedMetricSourceList;
        this.jmxExecutorService = jmxExecutorService;
        this.pathMapper = pathMapper;
    }

    public void initializeServerMetrics() {
        logMethods.info("Initializing Managed Metrics");

        logMethods.info("Creating JMX Executor Service " + jmxExecutorService);
        JmxMetric.setJmxConnectionPool(jmxExecutorService);

        for ( ManagedMetricSource s : managedMetricSourceList) {
            for ( ManagedMetric m : s.getManagedMetrics()) {
                logMethods.info("Setting up metric " + m);
                setupMetric(m);
            }
        }
        logMethods.info("Finished initializing Managed Metrics");
    }

    private void setupMetric(ManagedMetric m) {
        try {
            m.initializeMetrics(rootContext, pathMapper);
        } catch (Throwable t) {
            logMethods.error("Failed to set up managed metric " + m.getClass() + " " + m, t);
        }
    }


}

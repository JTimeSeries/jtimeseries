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
package com.od.jtimeseries.component.managedmetric.jmx.measurement;

import com.od.jtimeseries.timeseries.function.aggregate.AbstractDelegatingAggregateFunction;
import com.od.jtimeseries.timeseries.function.aggregate.AggregateFunction;
import com.od.jtimeseries.timeseries.function.aggregate.AggregateFunctions;
import com.od.jtimeseries.timeseries.function.aggregate.ChainedFunction;
import com.od.jtimeseries.util.numeric.DoubleNumeric;
import com.od.jtimeseries.util.numeric.Numeric;

/**
 * Created by IntelliJ IDEA.
* User: Nick Ebbutt
* Date: 04-Feb-2010
* Time: 10:10:52
*
*  Several jmx attributes represent total time (e.g. total process CPU time)
 *
*  For each time period, we want to measure the increase in total time as a
 * percentage of time elapsed over the period we are monitoring.
*
*  e.g. If the process has 500ms added to total process CPU time in a 1000ms period, then it has used 50% of a processor
*       (if there are more than one processors, this value may exceed 100%, which is expected)
*/
class JmxPercentageOfTimeFunction extends AbstractDelegatingAggregateFunction implements ChainedFunction {

    private long oldTotalTime;
    private long lastTriggerTime;

    public JmxPercentageOfTimeFunction() {
        this(0, 0);
    }

    private JmxPercentageOfTimeFunction(long oldTotalTime, long lastTriggerTime) {
        super(AggregateFunctions.SUM());
        this.oldTotalTime = oldTotalTime;
        this.lastTriggerTime = lastTriggerTime;
    }

    public Numeric calculateResult() {
        double result = Double.NaN;
        Numeric totalTime = super.calculateResult();
        if ( ! totalTime.isNaN() ) {
            long newTotalTime = totalTime.longValue();
            if ( oldTotalTime > 0 && componentNotRestarted(newTotalTime)) {
                long difference = newTotalTime - oldTotalTime;
                long timeInPeriod = System.currentTimeMillis() - lastTriggerTime;
                result = (100 * difference) / (double)timeInPeriod;
                //System.out.println("Calculated percentage " + result);
            }
            this.oldTotalTime = totalTime.longValue();
            lastTriggerTime = System.currentTimeMillis();
        }
        return DoubleNumeric.valueOf(result);
    }

    //if the total accumulated time has decreased we can assume the component we are monitoring
    //has been restarted
    private boolean componentNotRestarted(long newTotalTime) {
        return oldTotalTime < newTotalTime;
    }

    public AggregateFunction nextInstance() {
        return new JmxPercentageOfTimeFunction(oldTotalTime, lastTriggerTime);
    }
}

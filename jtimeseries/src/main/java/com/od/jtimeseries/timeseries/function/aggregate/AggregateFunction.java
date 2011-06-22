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
package com.od.jtimeseries.timeseries.function.aggregate;

import com.od.jtimeseries.util.numeric.Numeric;

public interface AggregateFunction {

    void addValue(Numeric value);

    void addValue(double value);

    void addValue(long value);

    Numeric getLastAddedValue();

    /**
     * @return a Numeric value which is the result of the aggregation.
     * If it is not possible to calculate a value, the Numeric returned should have numeric.isNaN() == true, or use Numeric.NaN
     */
    Numeric calculateAggregateValue();

    String getDescription();

    void clear();

    /**
     * This method is to enable the use of the prototype pattern, so that an AggregateFunction instance can be used
     * as a prototype. The function returned should be in its default initial state (equivalent to calling clear())
     */
    AggregateFunction newInstance();

    /**
     * This is similar to newInstance() in that a new function is created, but in this case the new instance may be initialized
     * with values from the current function. Functions are sometimes used in sequence with the previous function providing an initial
     * value to the next (in the case of Delta function, for example, where we need to keep track of the current value).
     *
     * @return a new instance of the AggregateFunction
     */
    AggregateFunction next();

}

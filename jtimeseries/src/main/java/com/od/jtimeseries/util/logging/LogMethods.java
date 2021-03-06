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
package com.od.jtimeseries.util.logging;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 03-Dec-2008
 * Time: 12:14:38
 *
 * JTimeseries logs to this interface, to enable the actual logging implementation to be
 * swapped out, adapters for log4j, java util logging and standard out are provided.
 *
 * The methods are selected to be identical to log4j where possible
 */
public interface LogMethods {

    void info(String s);

    boolean isInfoEnabled();

    void debug(String s);

    boolean isDebugEnabled();

    void debug(String s, Throwable t);

    void warn(String s);

    boolean isWarnEnabled();

    void warn(String s, Throwable t);

    void error(String s);

    void error(String s, Throwable t);

    void setLogLevel(LogLevel l);

    LogLevel getLogLevel();

    static enum LogLevel {
        ERROR(1),
        WARNING(2),
        INFO(3),
        DEBUG(4);

        int logLevel;

        LogLevel(int level) {
            this.logLevel = level;
        }

        public boolean equalsOrExceeds(LogLevel l) {
            return this.logLevel >= l.logLevel;
        }

        public static LogLevel getLogLevel(String logLevel) {
            logLevel = logLevel.trim();
            if ( logLevel.equalsIgnoreCase("error") ) {
                return ERROR;
            } else if ( logLevel.equalsIgnoreCase("warning") ) {
                return WARNING;
            } else if ( logLevel.equalsIgnoreCase("info") ) {
                return INFO;
            } else if ( logLevel.equalsIgnoreCase("debug") ) {
                return DEBUG;
            } else {
                System.err.println("Unknown Log Level " + logLevel + ". Will use INFO");
                return INFO;
            }
        }
    }
}

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
package com.od.jtimeseries.net.udp.message.properties;

import com.od.jtimeseries.net.udp.message.ClientAnnouncementMessage;
import com.od.jtimeseries.net.udp.message.MessageType;

import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 24-Jun-2009
 * Time: 12:48:40
 * To change this template use File | Settings | File Templates.
 *
 * A client may use this message type to announce it's presence to a timeseries server
 * 
 * The server may send a server announcement message back if it is running, which the client may use
 * to populate a list of active servers. This mechanism makes it possible to deploy a server without
 * preconfiguring or maintaining a list of clients
 */
public class PropertiesClientAnnouncementMessage extends PropertiesAnnouncementMessage implements ClientAnnouncementMessage {

    public static final String MESSAGE_TYPE = "ClientAnnouncementMessage";

    PropertiesClientAnnouncementMessage(int port, String description) {
        super(MESSAGE_TYPE);
        setProperty(PORT_KEY, String.valueOf(port));
        setProperty(DESCRIPTION_KEY, description);
    }

    PropertiesClientAnnouncementMessage(Properties p) {
        super(p);
    }

    public MessageType getMessageType() {
        return MessageType.CLIENT_ANNOUNCE;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ! (o instanceof ClientAnnouncementMessage)) return false;
        if (!super.equals(o)) return false;
        return true;
    }

    public String toString() {
        return getClass().getSimpleName() + "{" +
                super.toString() +
                "} ";
    }

}

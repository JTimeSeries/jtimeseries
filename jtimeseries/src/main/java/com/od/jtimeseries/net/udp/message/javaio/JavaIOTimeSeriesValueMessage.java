package com.od.jtimeseries.net.udp.message.javaio;

import com.od.jtimeseries.net.udp.message.MessageType;
import com.od.jtimeseries.net.udp.message.TimeSeriesValueMessage;
import com.od.jtimeseries.timeseries.Item;
import com.od.jtimeseries.timeseries.TimeSeriesItem;
import com.od.jtimeseries.util.numeric.DoubleNumeric;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 23/03/12
 * Time: 08:30
 */
public class JavaIOTimeSeriesValueMessage extends AbstractJavaIOMessage implements TimeSeriesValueMessage {

    private static byte[] HEADER_ACRONYM = new byte[] { 'V', '0' };

    private String path;
    private TimeSeriesItem item;

    public JavaIOTimeSeriesValueMessage() {}

    public JavaIOTimeSeriesValueMessage(String sourceHostname, String path, TimeSeriesItem item) {
        super(sourceHostname);
        this.path = path;
        this.item = item;
    }

    public MessageType getMessageType() {
        return MessageType.TS_VALUE;
    }

    public String getSeriesPath() {
        return path;
    }

    @Deprecated //send in a description message
    public String getDescription() {
        return null;
    }

    public TimeSeriesItem getTimeSeriesItem() {
        return item;
    }

    protected void doSerializeMessageBody(DataOutputStream d) throws IOException {
        d.writeUTF(path);
        d.writeLong(item.getTimestamp());
        d.writeDouble(item.getValue().doubleValue());
    }

    protected void doDeserialize(DataInputStream is, char acronymVersion) throws IOException {
        path = is.readUTF();
        long timestamp = is.readLong();
        double value = is.readDouble();
        item = new Item(timestamp,value);
    }

    protected byte[] getHeaderAcronym() {
        return HEADER_ACRONYM;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ! ( o instanceof TimeSeriesValueMessage)) return false;
        if (!super.equals(o)) return false;

        TimeSeriesValueMessage that = (TimeSeriesValueMessage) o;

        if (getTimeSeriesItem() != null ? !getTimeSeriesItem().equals(that.getTimeSeriesItem()) : that.getTimeSeriesItem() != null) return false;
        if (getSeriesPath() != null ? !getSeriesPath().equals(that.getSeriesPath()) : that.getSeriesPath() != null) return false;

        return true;
    }

    public int hashCode() {
        int result = getSeriesPath() != null ? getSeriesPath().hashCode() : 0;
        result = 31 * result + (getTimeSeriesItem() != null ? getTimeSeriesItem().hashCode() : 0);
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "{" +
                "path='" + getSeriesPath() + '\'' +
                ", item=" + getTimeSeriesItem() +
                super.toString() +
                "} ";
    }
}

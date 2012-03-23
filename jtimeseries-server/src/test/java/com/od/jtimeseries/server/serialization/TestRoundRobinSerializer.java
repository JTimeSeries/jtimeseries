package com.od.jtimeseries.server.serialization;

import com.od.jtimeseries.context.TimeSeriesContext;
import com.od.jtimeseries.context.impl.DefaultTimeSeriesContext;
import com.od.jtimeseries.server.timeseries.FilesystemTimeSeries;
import com.od.jtimeseries.server.util.ServerDefaults;
import com.od.jtimeseries.timeseries.Item;
import com.od.jtimeseries.timeseries.IndexedTimeSeries;
import com.od.jtimeseries.timeseries.TimeSeries;
import com.od.jtimeseries.timeseries.TimeSeriesItem;
import com.od.jtimeseries.timeseries.impl.DefaultTimeSeries;
import com.od.jtimeseries.timeseries.impl.RoundRobinTimeSeries;
import com.od.jtimeseries.util.numeric.DoubleNumeric;
import com.od.jtimeseries.util.time.Time;
import junit.framework.TestCase;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 17-May-2009
 * Time: 23:46:40
 * To change this template use File | Settings | File Templates.
 */
public class TestRoundRobinSerializer extends TestCase {

    private TimeSeriesSerializer serializer;
    private int SERIES_LENGTH = 10000;
    private FileHeader fileHeader = new FileHeader("test.id", "test series", SERIES_LENGTH);

    public void setUp() throws SerializationException {
        RoundRobinSerializer.setShutdownHandlingDisabled(true);        
        serializer = createTestSerializer();
        RoundRobinTimeSeries r = createTestSeries();
        serializer.writeSeries(fileHeader, r);

        serializer.getFile(fileHeader).deleteOnExit();
        serializer.getRootDirectory().deleteOnExit();        
    }

    public void testDeserialization() throws SerializationException {
        RoundRobinTimeSeries d = serializer.readSeries(fileHeader);
        RoundRobinTimeSeries r = createTestSeries();
        assertEquals(r, d);
    }

    public void testReadHeader() throws SerializationException {
        serializer.readHeader(fileHeader);
        assertEquals(FileHeader.DEFAULT_HEADER_START_LENGTH, fileHeader.getHeaderLength());
    }

    public void testWriteEmptyFile() throws SerializationException {
        RoundRobinTimeSeries s = new RoundRobinTimeSeries(5);
        serializer.writeSeries(fileHeader, s);
        assertEquals(-1, fileHeader.getCurrentHead());

        serializer.readHeader(fileHeader);
        assertEquals(-1, fileHeader.getCurrentHead());

        RoundRobinTimeSeries l = new RoundRobinTimeSeries(7);
        addNewItemsForTimestamps(l,1,2,3);
        serializer.appendToSeries(fileHeader, l);
        assertEquals(0, fileHeader.getCurrentHead());

        RoundRobinTimeSeries d = serializer.readSeries(fileHeader);
        assertEquals(l, d);
    }

    public void testAppend() throws SerializationException {
        RoundRobinTimeSeries l = new RoundRobinTimeSeries(7);
        addNewItemsForTimestamps(l, 5, 6);

        serializer.appendToSeries(fileHeader, l);
        assertEquals(0, fileHeader.getCurrentHead());
        assertEquals(6, fileHeader.getCurrentTail());

        serializer.readHeader(fileHeader);
        assertEquals(0, fileHeader.getCurrentHead());
        assertEquals(6, fileHeader.getCurrentTail());

        RoundRobinTimeSeries d = serializer.readSeries(fileHeader);
        RoundRobinTimeSeries c = createTestSeries();
        addNewItemsForTimestamps(c, 5, 6);
        assertEquals(c, d);
    }

    public void testAppend2() throws SerializationException {
        //test wrap around - 8 should end up at position zero
        RoundRobinTimeSeries l = new RoundRobinTimeSeries(7);
        addNewItemsForTimestamps(l,5,6,7,8);

        serializer.appendToSeries(fileHeader, l);
        assertEquals(1, fileHeader.getCurrentHead());
        assertEquals(1, fileHeader.getCurrentTail());

        serializer.readHeader(fileHeader);
        assertEquals(1, fileHeader.getCurrentHead());
        assertEquals(1, fileHeader.getCurrentTail());

        RoundRobinTimeSeries d = serializer.readSeries(fileHeader);
        RoundRobinTimeSeries c = createTestSeries();
        addNewItemsForTimestamps(c,5,6,7,8);
        assertEquals(c, d);

        //rr strucure is now full, both head and tail advance when we add
        l.clear();
        l.addItem(createItemForTimestamp(9));
        serializer.appendToSeries(fileHeader, l);

        assertEquals(2, fileHeader.getCurrentHead());
        assertEquals(2, fileHeader.getCurrentTail());

        d = serializer.readSeries(fileHeader);
        c.addItem(createItemForTimestamp(9));
        assertEquals(c, d);

        //try adding a random number of values which may exceed the max capacity
        //we should just wrap around however many times required and end up with the last 7 items
        int toAdd = (int) (Math.random() * 100);
        long[] itemsForTimestamps = new long[toAdd];
        for ( int loop=0; loop < toAdd; loop++) {
            itemsForTimestamps[loop] = loop + 10;
        }

        l.clear();
        addNewItemsForTimestamps(l, itemsForTimestamps);
        serializer.appendToSeries(fileHeader, l);
        d = serializer.readSeries(fileHeader);
        addNewItemsForTimestamps(c, itemsForTimestamps);
        assertEquals(c, d);
    }

    public void testFilesystemTimeSeries() throws SerializationException {
        TimeSeriesContext c = new DefaultTimeSeriesContext().createContext("test");
        FilesystemTimeSeries series = new FilesystemTimeSeries(c.getPath(), "id", "description", serializer, SERIES_LENGTH, Time.seconds(10), Time.seconds(10));
        c.addChild(series);
        FileHeader fileHeader = series.getFileHeader();
        assertEquals(4, series.size());
        assertEquals(4, series.getItem(3).longValue());

        addNewItemsForTimestamps(series, 5,6);
        assertEquals(6, series.size());
        series.flush();
        assertEquals(6, series.getItem(5).longValue());

        IndexedTimeSeries s = serializer.readSeries(fileHeader);
        assertEquals(6, s.getItem(5).longValue());

        series.removeItem(createItemForTimestamp(3));
        series.removeItem(createItemForTimestamp(4));
        assertEquals(4, series.size());
        series.flush();

        s = serializer.readSeries(fileHeader);
        assertEquals(4, s.size());
        DefaultTimeSeries l = new DefaultTimeSeries();
        addNewItemsForTimestamps(l, 1,2,5,6);
        assertEquals(l, s);
    }

    //usually we use a write behind cache to write several updates in one go so this is the worst case scenario
    public void testAppendSpeed() throws SerializationException {
        long startTime = System.currentTimeMillis();
        for ( int loop=0; loop < 1000; loop++) {
            RoundRobinTimeSeries s = new RoundRobinTimeSeries(7);
            s.addItem(new Item(loop, (double)loop));
            serializer.appendToSeries(fileHeader, s);
        }
        long endTime = System.currentTimeMillis();
        assertEquals(7, fileHeader.getCurrentSeriesSize());
        System.out.println("Time to append to 1000 series " + (endTime - startTime));
        //expected more like < 200
        assertTrue("Test append speed", (endTime - startTime) < 5000);
    }

    public void testPathMigration() throws SerializationException {
        assertEquals("test.id", fileHeader.getPath());

        serializer.migratePath(fileHeader, "new.path");
        assertTrue(serializer.fileExists(fileHeader));
        serializer.getFile(fileHeader).deleteOnExit();
        assertTrue(fileHeader.getPath().equals("new.path"));

        FileHeader header2 = new FileHeader("test.id", "test", 100);
        assertFalse(serializer.fileExists(header2));

        try {
            serializer.migratePath(header2, "new.path");
            fail("Should fail to migrate to a path for which a file exists already");
        } catch (SerializationException s) {
        }
    }

    private RoundRobinTimeSeries createTestSeries() {
        RoundRobinTimeSeries r = new RoundRobinTimeSeries(7);
        addNewItemsForTimestamps(r,1,2,3,4);
        return r;
    }

    public static TimeSeriesSerializer createTestSerializer() throws SerializationException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), "testtimeseries");
        if ( ! tmpDir.isDirectory()) {
            if ( ! tmpDir.mkdir() ) {
                fail("Failed to create temporary timeseries directory");
                throw new SerializationException("Failed to create test dir");
            }
        }
        tmpDir.deleteOnExit();
        return new RoundRobinSerializer(tmpDir, ServerDefaults.DEFAULT_TIMESERIES_SUFFIX);
    }

    private void addNewItemsForTimestamps(TimeSeries s, long... timestampsForItems) {
        for (long timestampsForItem : timestampsForItems) {
            s.addItem(createItemForTimestamp(timestampsForItem));
        }
    }

    public static TimeSeriesItem createItemForTimestamp(long timestampsForItem) {
        return new Item(timestampsForItem, DoubleNumeric.valueOf(timestampsForItem));
    }


}

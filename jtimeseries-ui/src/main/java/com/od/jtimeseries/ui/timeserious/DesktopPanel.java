package com.od.jtimeseries.ui.timeserious;

import com.od.jtimeseries.net.udp.TimeSeriesServerDictionary;
import com.od.jtimeseries.net.udp.UdpPingHttpServerDictionary;
import com.od.jtimeseries.ui.timeserious.config.TimeSeriousConfig;
import com.od.jtimeseries.ui.visualizer.VisualizerConfiguration;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 24-Nov-2010
 * Time: 09:29:26
 * To change this template use File | Settings | File Templates.
 */
public class DesktopPanel extends JPanel implements TimeSeriousDesktop {

    private TimeSeriesServerDictionary timeSeriesServerDictionary = new UdpPingHttpServerDictionary();
    private TimeSeriesDesktopPane desktopPane = new TimeSeriesDesktopPane(timeSeriesServerDictionary);

    public DesktopPanel() {
        super(new BorderLayout());
        add(desktopPane, BorderLayout.CENTER);
    }

    public void createAndAddVisualizer() {
        desktopPane.createAndAddVisualizer();
    }

    public List<VisualizerConfiguration> getVisualizerConfigurations() {
        return desktopPane.getVisualizerConfigurations();
    }

    public void prepareConfigForSave(TimeSeriousConfig config) {
        config.setVisualizerConfigurations(getVisualizerConfigurations());
    }

    public void restoreConfig(TimeSeriousConfig config) {
        desktopPane.addVisualizers(config.getVisualizerConfigurations());
    }
}

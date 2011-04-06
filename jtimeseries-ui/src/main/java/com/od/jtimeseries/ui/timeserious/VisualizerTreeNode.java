package com.od.jtimeseries.ui.timeserious;

import com.od.jtimeseries.ui.selector.tree.AbstractSeriesSelectionTreeNode;
import com.od.jtimeseries.ui.util.ImageUtils;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 11/03/11
 * Time: 12:18
 */
public class VisualizerTreeNode  extends AbstractSeriesSelectionTreeNode {

    private VisualizerContext identifiable;

    public VisualizerTreeNode(VisualizerContext identifiable) {
        this.identifiable = identifiable;
    }

    public String toString() {
        return identifiable.toString();
    }

    @Override
    protected VisualizerContext getIdentifiable() {
        return identifiable;
    }

    protected Icon getIcon() {
        return ImageUtils.ADD_TO_VISUALIZER_16x16;
    }

    @Override
    public boolean isSelected() {
        return false;
    }
}

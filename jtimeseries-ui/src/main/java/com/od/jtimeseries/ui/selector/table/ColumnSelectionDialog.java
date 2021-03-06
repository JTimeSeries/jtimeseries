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
package com.od.jtimeseries.ui.selector.table;

import com.od.jtimeseries.context.ContextProperties;
import com.od.jtimeseries.ui.util.ImageUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 27-Feb-2010
 * Time: 22:17:43
 * To change this template use File | Settings | File Templates.
 */
public class ColumnSelectionDialog extends JDialog {

    private TableColumnManager tableColumnManager;

    public ColumnSelectionDialog(Frame owner, JComponent parentComponent, TableColumnManager tableColumnManager) {
        super(owner);
        setTitle("Choose Columns");
        setModal(true);
        this.tableColumnManager = tableColumnManager;
        buildDialog();
        setLocationRelativeTo(parentComponent);
        setIconImage(ImageUtils.TABLE_COLUMN_ADD_16x16.getImage());
    }

    private void buildDialog() {
        JPanel columnPanel = createColumnsPanel();
        JComponent buttonPanel = createButtonPanel();

        setLayout(new BorderLayout());
        add(columnPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
    }

    private JComponent createButtonPanel() {
        Box b = Box.createHorizontalBox();
        b.add(Box.createHorizontalGlue());
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        b.add(okButton);
        return b;
    }

    private JPanel createColumnsPanel() {
        JPanel columnPanel = new JPanel();
        columnPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        String[] columnNames = tableColumnManager.getAllColumnNamesInTableModel();
        Arrays.sort(columnNames);

        int rows = 8;
        int cols = (int)Math.ceil(columnNames.length / (double)rows);
        columnPanel.setLayout(new GridLayout(rows, cols, 10, 10));

        String displayName;
        for ( final String name : columnNames) {
            displayName = name;
            if (ContextProperties.isSummaryStatsProperty(displayName)) {
                displayName = ContextProperties.parseStatisticName(displayName);
            }
            final JCheckBox checkBox = new JCheckBox(displayName, tableColumnManager.isInTableModel(name));
            checkBox.setToolTipText(tableColumnManager.getColumnDescription(name));
            columnPanel.add(checkBox);

            checkBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if ( checkBox.isSelected() ) {
                        tableColumnManager.addColumn(name);
                    } else {
                        tableColumnManager.removeColumn(name);
                    }
                }
            });
        }
        return columnPanel;
    }
}

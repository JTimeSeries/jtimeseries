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
package com.od.jtimeseries.ui.uicontext;

import com.od.jtimeseries.ui.config.ConfigManagerForTimeSerious;
import com.od.jtimeseries.ui.config.ExportableConfig;
import com.od.jtimeseries.ui.config.ExportableConfigHolder;
import com.od.jtimeseries.ui.identifiable.DesktopContext;
import com.od.jtimeseries.util.logging.LogMethods;
import com.od.jtimeseries.util.logging.LogUtils;
import od.configutil.ConfigManager;
import od.configutil.util.ConfigManagerException;
import od.configutil.sinkandsource.FileSink;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Nick
 * Date: 08/01/11
 * Time: 09:41
 * To change this template use File | Settings | File Templates.
 */
public class IdentifiableTransferable implements Transferable {

    private static LogMethods logMethods = LogUtils.getLogMethods(IdentifiableTransferable.class);

    private static final String LOCAL_SELECTIONS_TRANSFER_DATA_CLASS = LocalSelectionsTransferData.class.getName();

    private static final String listType = DataFlavor.javaJVMLocalObjectMimeType +
            ";class=" + LOCAL_SELECTIONS_TRANSFER_DATA_CLASS;
    public static DataFlavor LOCAL_SELECTIONS_FLAVOR;

    static {
        try {
            LOCAL_SELECTIONS_FLAVOR = new DataFlavor(listType);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private LocalSelectionsTransferData selections;

    public IdentifiableTransferable(LocalSelectionsTransferData selectionsModel) {
        this.selections = selectionsModel;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return ( selections.isSelectionLimitedToType(ExportableConfigHolder.class)) ?
         new DataFlavor[] { DataFlavor.javaFileListFlavor, LOCAL_SELECTIONS_FLAVOR} :
         new DataFlavor[] {LOCAL_SELECTIONS_FLAVOR};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        boolean result = false;
        if (flavor.equals(LOCAL_SELECTIONS_FLAVOR)) {
            result = true;
        } else if ( flavor.equals(DataFlavor.javaFileListFlavor )) {
            result = selections.isSelectionLimitedToType(DesktopContext.class);
        }
        return result;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        Object result = null;
        if ( flavor == LOCAL_SELECTIONS_FLAVOR) {
            result = selections;
        } else if ( flavor == DataFlavor.javaFileListFlavor ) {
            //System.out.println("Creating file list");
            List<ExportableConfigHolder> visualizerContexts = selections.getSelected(ExportableConfigHolder.class);
            ConfigManager m = new ConfigManagerForTimeSerious();
            List<File> files = new LinkedList<File>();
            for (ExportableConfigHolder n : visualizerContexts) {
                ExportableConfig c = n.getExportableConfig();
                String encodedTitle = URLEncoder.encode(n.getDefaultFileName(), "UTF-8");

                File tmpDir = new File(System.getProperty("java.io.tmpdir"));
                File tmpFile = new File(tmpDir, encodedTitle + ".xml");

                try {
                    m.saveConfig("exportableConfig", c, new FileSink(tmpFile));
                    files.add(tmpFile);
                    tmpFile.deleteOnExit();
                } catch (ConfigManagerException e) {
                    logMethods.error("Failed to write temporary exportable config", e);
                }
            }
            result = files;
        }
        return result;
    }

}

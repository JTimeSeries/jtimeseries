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
package com.od.jtimeseries.ui.config;

import com.jidesoft.dialog.ButtonEvent;
import com.jidesoft.dialog.ButtonNames;
import com.jidesoft.dialog.PageList;
import com.jidesoft.wizard.DefaultWizardPage;
import com.jidesoft.wizard.WizardDialog;
import com.jidesoft.wizard.WizardStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 18-Nov-2010
 * Time: 14:37:00
 * To change this template use File | Settings | File Templates.
 */
public class ConfigDirectorySelector {

    private DirectorySelectionWizard wizard;

    public ConfigDirectorySelector(JFrame mainFrame) {
        WizardStyle.setStyle(WizardStyle.JAVA_STYLE);
        wizard = new DirectorySelectionWizard(mainFrame);
    }

    public void showSelectorDialog() {
        wizard.setVisible(true);
    }

    public File getSelectedDirectory() {
        return wizard.getSelectedDirectory();
    }

    private class DirectorySelectionWizard extends WizardDialog {

        private ConfigFilePage welcomePage = new ConfigFilePage(
            "Choose a directory for config files",
            "",
             this
        );

        private DirectorySelectionWizard(Frame frame) throws HeadlessException {
            super(frame, "Select a Config Directory");
            setSize(600, 600);
            setLocationRelativeTo(frame);
            setStepsPaneNavigable(false);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            PageList p = new PageList();
            p.append(welcomePage);

            setPageList(p);
        }

        private File getSelectedDirectory() {
            return welcomePage.getSelectedDirectory();
        }
    }

    private class ConfigFilePage extends DefaultWizardPage {

        private File selectedDirectory;
        private WizardDialog wizardDialog;

        public ConfigFilePage(String title, String description, WizardDialog wizardDialog) {
            super(title, description);
            this.wizardDialog = wizardDialog;
        }

        @Override
        public JComponent createWizardContent() {
            final JFileChooser f = new JFileChooser();
            f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            f.setApproveButtonText("Select");
            f.setApproveButtonMnemonic('s');

            f.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if ( e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                            selectedDirectory = f.getSelectedFile();
                            wizardDialog.dispose();
                        }
                    }
                }
            );
            return f;
        }

        @Override
        public void setupWizardButtons() {
            fireButtonEvent(ButtonEvent.HIDE_BUTTON, ButtonNames.BACK);
            fireButtonEvent(ButtonEvent.HIDE_BUTTON, ButtonNames.NEXT);
            fireButtonEvent(ButtonEvent.HIDE_BUTTON, ButtonNames.CANCEL);
            fireButtonEvent(ButtonEvent.HIDE_BUTTON, ButtonNames.FINISH);
        }

        public File getSelectedDirectory() {
            return selectedDirectory;
        }
    }

}

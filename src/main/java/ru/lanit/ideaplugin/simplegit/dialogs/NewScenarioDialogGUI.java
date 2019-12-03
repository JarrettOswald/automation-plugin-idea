package ru.lanit.ideaplugin.simplegit.dialogs;

import javax.swing.*;

public class NewScenarioDialogGUI extends JDialog {
    private JPanel contentPane;
    private JTextField scenarioName;
    private JTextField featureName;
    private JTextField tags;

    public NewScenarioDialogGUI() {
        setContentPane(contentPane);
        setModal(true);
        pack();
        validate();
   }
}

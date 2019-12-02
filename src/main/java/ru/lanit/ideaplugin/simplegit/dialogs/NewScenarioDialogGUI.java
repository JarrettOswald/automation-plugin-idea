package ru.lanit.ideaplugin.simplegit.dialogs;

import javax.swing.*;

public class NewScenarioDialogGUI extends JDialog {
    private JPanel contentPane;
    private JTextField featureName;
    private JTextField scenarioName;
    private JTextField tags;

    public JTextField getFeatureName() {
        return featureName;
    }

    public JTextField getScenarioName() {
        return scenarioName;
    }

    public JTextField getTags() {
        return tags;
    }

    public NewScenarioDialogGUI() {
    }
}

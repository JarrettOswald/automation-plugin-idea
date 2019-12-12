package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import ru.lanit.ideaplugin.simplegit.SimpleGitApplicationComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class FeatureComboBoxAction extends ComboBoxAction {

    ComboBoxModel featureModel;
    String[] results;
    String phrase;
    Application application = ApplicationManager.getApplication();
    SimpleGitApplicationComponent simpleGitComponent = application.getComponent(SimpleGitApplicationComponent.class);
    JComboBox resultsComboBox = new JComboBox();
    JPanel featuresPanel = new JPanel();
    JLabel textLabel = new JLabel();

    //When the menu item is called under the Window menu,
    //the features are retrieved in a background task,
    //this is essentially a "refresh" action, because the "createCustomComponent"
    //method, called at the creation of the action, initially fills the JComboBox:
    public void actionPerformed(AnActionEvent event) {
        getFeatures(event.getProject());
    }

    //Register the action into the DefaultActionGroup,
    //via its "id", as expressed in the XML snippet above:
    protected DefaultActionGroup createPopupActionGroup(JComponent jComponent) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(ActionManager.getInstance().getAction("ru.lanit.ideaplugin.simplegit.actions.FeatureSelectorAction"));
        return group;
    }

    //This method is called when the action is created,
    //which is at startup, via the XML tags.
    //It gets the features and then creates the custom component,
    //a JPanel, in the toolbar:
    public JComponent createCustomComponent(Presentation presentation) {
        getFeatures(null);
        featuresPanel.setBorder(BorderFactory.createLineBorder(Color.orange, 2));
        featuresPanel.add(resultsComboBox, BorderLayout.WEST);
        //For testing purposes, I created a JLabel, which displayed the URL:
        //youTubePanel.add(textLabel, BorderLayout.EAST);
        return featuresPanel;
    }

    //Utility method for retrieving the features,
    //notice that ApplicationManager.getApplication().invokeLater() is used,
    //which creates our background task, so that the ui isn't frozen
    //while features are searched:
    public void getFeatures(Project project) {
        application.invokeLater(new Runnable() {
            public void run() {
                results = simpleGitComponent.searchFeatures(project);
                featureModel = new DefaultComboBoxModel(results);
                resultsComboBox.setModel(featureModel);
                resultsComboBox.addItemListener(new FeatureSelectionListener());
                resultsComboBox.setToolTipText(results.length + " results for:\"" + project.getBasePath() + "\"");
            }
        });
    }

    //When an item is selected, the browser needs to start, with the movie's URL:
    private class FeatureSelectionListener implements ItemListener {
        public void itemStateChanged(ItemEvent evt) {
            JComboBox cb = (JComboBox) evt.getSource();
            if (evt.getStateChange() == ItemEvent.SELECTED) {
                simpleGitComponent.selectFeature((String) cb.getSelectedItem());
            }
        }
    }

}
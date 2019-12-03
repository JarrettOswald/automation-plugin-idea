package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import cucumber.runtime.model.CucumberFeature;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.SimpleGitPlugin;

public class ScenarioSelector extends AbstractComboBoxAction<CucumberFeature> {

    public ScenarioSelector() {
        super();
        System.out.println("Create scenario selector");
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
        Presentation presentation = event.getPresentation();
        Project project = event.getProject();
        if (project == null) {
            presentation.setEnabled(false);
        } else {
            presentation.setEnabled(true);
            showDisabledActions(true);
            setItems(SimpleGitPlugin.getPluginFor(event).getFeatures(), null);
        }
    }

    protected void update(CucumberFeature item, Presentation presentation, boolean popup) {
        presentation.setEnabled(true);
        if (item != null) {
            System.out.println("Updated item " + item.getGherkinFeature().getName());
            if (popup) {
                presentation.setText(item.getGherkinFeature().getName());
            } else {
                presentation.setText(item.getGherkinFeature().getName());
            }
            /*if (!popup && item.getScenarioName().contains("2")) {
                presentation.setText(item.getScenarioName());
            }
            else if (!popup) {
                presentation.setText(item.getScenarioName());
            }
            else {
                presentation.setText("      " + item.getScenarioName());
            }*/
        }
        else {
//            presentation.setText("[None]");
        }
    }

    protected boolean selectionChanged(CucumberFeature item) {
        System.out.println("New scenario selected: " + item.getPath());
        return true;
    }
}

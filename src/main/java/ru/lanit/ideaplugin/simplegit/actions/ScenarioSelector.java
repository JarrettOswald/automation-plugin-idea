package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.designer.actions.AbstractComboBoxAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.scenario.ScenarioWrapper;

import java.util.Arrays;

public class ScenarioSelector extends AbstractComboBoxAction<ScenarioWrapper> {

    public ScenarioSelector() {
        setItems(Arrays.asList(new ScenarioWrapper("Scenario 1"), new ScenarioWrapper("Scenario 2")), null);
        System.out.println("Create scenario selector");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("ACTION");
    }

    protected void update(ScenarioWrapper item, Presentation presentation, boolean popup) {
        System.out.println("Updated item " + item);
//        presentation.setEnabled(true);
        if (item != null) {
            if (!popup && item.getScenarioName().contains("2")) {
                presentation.setText(item.getScenarioName());
            }
            else if (!popup) {
                presentation.setText(item.getScenarioName());
            }
            else {
                presentation.setText("      " + item.getScenarioName());
            }
        }
        else {
            presentation.setText("[None]");
        }
    }

    protected boolean selectionChanged(ScenarioWrapper item) {
        System.out.println("New scenario: " + item);
        return true;
    }
}

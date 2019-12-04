package ru.lanit.ideaplugin.simplegit;

import com.intellij.designer.actions.AbstractComboBoxAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformIcons;
import cucumber.runtime.model.CucumberFeature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.lanit.ideaplugin.simplegit.actions.ScenarioSelector;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ScenarioList {
    private static ConcurrentHashMap<Presentation, ScenarioList> scenarioListByPresentation = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<JComponent, ScenarioList> scenarioListByJComponent = new ConcurrentHashMap<>();

    private static final Icon CHECKED = PlatformIcons.CHECK_ICON;

    private SimpleGitPlugin plugin;
    private List<CucumberFeature> myItems = Collections.emptyList();
    private CucumberFeature mySelection;
    private Presentation presentation;
    private boolean showDisabledActions;

    public ScenarioList(Presentation presentation) {
        this.presentation = presentation;
        presentation.setEnabled(true);
    }

    public static ScenarioList getScenarioListFor(Presentation presentation) {
        System.out.println("Try get scenario list by presentation");
        return scenarioListByPresentation.computeIfAbsent(presentation, ScenarioList::new);
    }

    public void registerPlugin(SimpleGitPlugin plugin) {
        this.plugin = plugin;
        setItems(plugin.getFeatures(), null);
    }

    public static void registerJComponent(Presentation presentation, JComponent button) {
        ScenarioList scenarioList = scenarioListByPresentation.get(presentation);
        scenarioList.update();
        scenarioListByJComponent.put(button, scenarioList);
    }

    public static DefaultActionGroup createPopupActionGroup(JComponent button) {
        ScenarioList scenarioList = scenarioListByJComponent.get(button);
        if (scenarioList != null) {
            return scenarioList.createPopupActionGroup();
        }
        System.out.println("Try to get unregistered ScenarioList");
        return null;
    }

    public void setItems(List<CucumberFeature> items, @Nullable CucumberFeature selection) {
        myItems = items;
        setSelection(selection);
    }

    public CucumberFeature getSelection() {
        return mySelection;
    }

    public void setSelection(CucumberFeature selection) {
        mySelection = selection;
        if (selection == null && !myItems.isEmpty()) {
            mySelection = myItems.get(0);
        }
        update();
    }

    public void clearSelection() {
        mySelection = null;
        update();
    }

    public void showDisabledActions(boolean value) {
        showDisabledActions = value;
    }

    public void update() {
        update(mySelection, presentation, false);
    }

    private DefaultActionGroup createPopupActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        for (final CucumberFeature item : myItems) {
            if (addSeparator(actionGroup, item)) {
                continue;
            }

            AnAction action = new AnAction() {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    if (mySelection != item && selectionChanged(item)) {
                        mySelection = item;
                        ScenarioList.this.update(item, presentation, false);
                    }
                }
            };
            actionGroup.add(action);

            Presentation presentation = action.getTemplatePresentation();
            presentation.setIcon(mySelection == item ? CHECKED : null);
            update(item, presentation, true);
        }

        return actionGroup;
    }

    protected boolean addSeparator(DefaultActionGroup actionGroup, CucumberFeature item) {
        return false;
    }

    protected void update(CucumberFeature item, Presentation presentation, boolean popup) {
        presentation.setEnabled(true);
        if (item != null) {
            System.out.println("Updated item " + item.getGherkinFeature().getName());
            if (popup) {
                // For list element
                presentation.setText(item.getGherkinFeature().getName());
            } else {
                // For selected value in ComboBox
                presentation.setText(item.getGherkinFeature().getName());
            }
        }
        else {
            // For empty element
//            presentation.setText("[None]");
        }
    }

    protected boolean selectionChanged(CucumberFeature item) {
        System.out.println("New scenario selected: " + item.getPath());
        return true;
    }

    public boolean isShowDisabledActions() {
        return showDisabledActions;
    }
}

package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.util.PlatformIcons;
import cucumber.runtime.model.CucumberFeature;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.ScenarioList;
import ru.lanit.ideaplugin.simplegit.SimpleGitPlugin;

import javax.swing.*;
import java.awt.*;

public class ScenarioSelector extends ComboBoxAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        SimpleGitPlugin.registerScenarioComboBox(event);
    }

    @NotNull
    @Override
    protected DefaultActionGroup createPopupActionGroup(JComponent button) {
        return ScenarioList.createPopupActionGroup(button);
    }

    @NotNull
    @Override
    public JComponent createCustomComponent(Presentation presentation) {
//    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {

        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(
                createComboBoxButton(presentation),
                new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 1, 2, 1), 0, 0));
        ScenarioList.registerJComponent(presentation, panel);
        return panel;
    }

    @Override
    protected ComboBoxButton createComboBoxButton(Presentation presentation) {
        ScenarioList scenarioList = ScenarioList.getScenarioListFor(presentation);
        if (scenarioList.isShowDisabledActions()) {
            return new ComboBoxButton(presentation) {
                @Override
                protected JBPopup createPopup(Runnable onDispose) {
                    ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, createPopupActionGroup(this), getDataContext(), true, onDispose, getMaxRows());
                    popup.setMinimumSize(new Dimension(getMinWidth(), getMinHeight()));
                    return popup;
                }
            };
        }
        return super.createComboBoxButton(presentation);
    }

}

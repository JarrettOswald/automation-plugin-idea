package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;
import ru.lanit.ideaplugin.simplegit.ScenarioList;
import ru.lanit.ideaplugin.simplegit.SimpleGitPlugin;

import javax.swing.*;
import java.awt.*;

public class ScenarioSelectorAction extends ComboBoxAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        System.out.println("1");
        SimpleGitPlugin.registerScenarioComboBox(event);
    }

    @NotNull
    @Override
    public JComponent createCustomComponent(Presentation presentation) {
//    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
        System.out.println("2");
        JPanel panel = new JPanel(new GridBagLayout());
        ComboBoxButton button = createComboBoxButton(presentation);
        ScenarioList.registerJComponent(presentation, button);
        panel.add(
                button,
                new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 1, 2, 1), 0, 0));
        return panel;
    }

    @Override
    protected ComboBoxButton createComboBoxButton(Presentation presentation) {
        System.out.println("Create combobox button");
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

    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent button) {
        return ScenarioList.createPopupActionGroup(button);
    }

}

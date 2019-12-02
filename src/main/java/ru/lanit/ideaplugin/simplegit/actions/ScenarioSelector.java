package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.designer.actions.AbstractComboBoxAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.lanit.ideaplugin.simplegit.SimpleGitPlugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScenarioSelector extends AbstractComboBoxAction<ScenarioSelector.T> {

    protected class T {
        private String s;
        public T(String s) {
            this.s = s;
        }
    }

    public ScenarioSelector() {
        setItems(Arrays.asList(new T("Scenario 1"), new T("Scenario 2")), null);
        System.out.println("Create scenario selector");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("ACTION");
    }

    protected void update(T item, Presentation presentation, boolean popup) {
        System.out.println("Updated item " + item);
//        presentation.setEnabled(true);
        if (item != null) {
            if (!popup && item.s.contains("2")) {
                presentation.setText(item.s);
            }
            else if (!popup) {
                presentation.setText(item.s);
            }
            else {
                presentation.setText("      " + item.s);
            }
        }
        else {
            presentation.setText("[None]");
        }
    }

    protected boolean selectionChanged(T item) {
        System.out.println("New scenario: " + item);
        return true;
    }
}

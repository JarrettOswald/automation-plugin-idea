package ru.lanit.ideaplugin.simplegit.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class NewScenarioDialog extends DialogWrapper {
    public NewScenarioDialog(@Nullable Project project) {
        super(project);
        init();
        setTitle("Create new scenario");
    }
/*
    private class MyCustomAction extends DialogWrapperAction {
        protected MyCustomAction() {
            super("Label");
            putValue(Action.NAME, "Label");
        }

        @Override
        protected void doAction(ActionEvent e) {
            if (doValidate() == null) {
                getOKAction().setEnabled(isOkEnabled());
            }
            // set implementation specific values to signal that this custom button was the cause for closing the dialog
            // .....
            doOKAction();
        }
    }
*/
    boolean isOkEnabled() {
        // return true if dialog can be closed
        return true;
    }


    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return new NewScenarioDialogGUI().getRootPane();
    }
/*
    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();
        // return right hand side action buttons
        return new Action[] { myPastePlainTextAction, myPasteHtmlAction, myOkAction, getCancelAction() };
    }

    @NotNull
    protected Action[] createLeftSideActions() {
        // return left hand side action buttons
        return new Action[] { copyToDefaultsAction, copyFromDefaultsAction, resetAction, };
    }*/
}
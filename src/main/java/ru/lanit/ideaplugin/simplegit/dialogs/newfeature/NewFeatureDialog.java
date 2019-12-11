package ru.lanit.ideaplugin.simplegit.dialogs.newfeature;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class NewFeatureDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField scenarioName;
    private JTextField featureName;
    private JTextField tags;

    public NewFeatureDialog(@Nullable Project project) {
        super(project);
        setModal(true);
        pack();
        validate();
        init();
        setTitle("Create new scenario");
        setResizable(false);
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
        return contentPane;
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
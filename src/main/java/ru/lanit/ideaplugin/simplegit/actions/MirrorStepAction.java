package ru.lanit.ideaplugin.simplegit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

public class MirrorStepAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        CaretModel caretModel = editor.getCaretModel();
        e.getPresentation().setEnabledAndVisible(caretModel.getCurrentCaret().hasSelection());
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        CaretModel caretModel = editor.getCaretModel();
        final Document document = editor.getDocument();
        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        int start = primaryCaret.getSelectionStart();
        int end = primaryCaret.getSelectionEnd();

        if (caretModel.getCurrentCaret().hasSelection()) {
            String query = caretModel.getCurrentCaret().getSelectedText();
            WriteCommandAction.runWriteCommandAction(project, () ->
                    document.replaceString(start, end, mirror(query))
            );
            primaryCaret.removeSelection();
        }
    }

    private String mirror(String text) {
        String textPlusMirror = "";
        if (text.contains("ввести"))
            textPlusMirror = text.replace("ввести", "введено");
        else if (text.contains("выбрать") && !text.contains("радиокнопку"))
            textPlusMirror = text.replace("выбрать", "выбрано");
        else if (text.contains("выбрать") && text.contains("радиокнопку"))
            textPlusMirror = text.replace("выбрать радиокнопку", "радиокнопка") + " выбрана";
        else return text;
        return text + "\n    " + textPlusMirror.replaceAll("^ *\\S+", "Тогда").trim();

    }
}

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
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

import java.util.Objects;
import java.util.Scanner;

public class ReplaceStepsAction extends AnAction {
    public static String indentation = "   ";
    public static boolean isSetStepVoid = false;
    public static boolean isStepSeparator = false;
    StatusBar statusBar = null;

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        CaretModel caretModel = editor.getCaretModel();
        final Document document = editor.getDocument();
        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        int start = primaryCaret.getSelectionStart();
        int end = primaryCaret.getSelectionEnd();
        statusBar = WindowManager.getInstance().getStatusBar(Objects.requireNonNull(e.getProject()));
        if (caretModel.getCurrentCaret().hasSelection()) {
            String query = caretModel.getCurrentCaret().getSelectedText();
            WriteCommandAction.runWriteCommandAction(project, () ->
                    document.replaceString(start, end, featureReplace(query))
            );
            primaryCaret.removeSelection();
        }
    }

    private String featureReplace(String text) {
        StringBuilder result = new StringBuilder();
        Scanner scanner = new Scanner(text);
        String stepSeparator = isStepSeparator ? "\n" : "";
        int i = 1;
        boolean isFirstFeature = false;
        boolean isVoidLine = false;

        String line = scanner.nextLine();
        if (line.contains("language:")) {
            isFirstFeature = true;
        } else {
            try {
                i = Integer.parseInt(line.trim().replaceAll("[^\\d]", "")) + 1;
            } catch (NumberFormatException e) {
                JBPopupFactory.getInstance()
                        .createHtmlTextBalloonBuilder("В первой строке не найдено номера шага", MessageType.ERROR, null)
                        .setFadeoutTime(7500)
                        .createBalloon()
                        .show(RelativePoint.getCenterOf(statusBar.getComponent()),
                                Balloon.Position.atRight);
                return text;
            }
        }
        result.append(indentation).append(line).append(scanner.hasNextLine() ? "\n" : "");

        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            if (isFirstFeature) {
                result.append(line).append("\n");
                while (!(line = scanner.nextLine()).contains("Сценарий:"))
                    result.append(line).append("\n");
                result.append(line).append("\n");
                line = scanner.nextLine();
                isFirstFeature = false;
            }

            if (isVoidLine && !line.matches("^ *$") && !line.matches(" *#+\\d* *.*")) {
                result.append(stepSeparator).append(indentation).append("#").append(i).append("\n");
                i++;
            }
            isVoidLine = false;
            //Пропускаем
            if (line.matches(" *#todo.*")) {
                result.append(line).append("\n");
                //Пропускаем
            } else if (!line.matches("^( *# *\\d+.*)|(^ *$)|( *# *)$")) {
                result.append(line).append("\n");
                //Изменяем
            } else if (line.matches(" *#+\\d* *.*")) {
                result.append(stepSeparator).append(line.replaceAll(" *# *\\d*", indentation + "#" + i)).append("\n");
                i++;
                //Изменяем в пустых строках
            } else if (isSetStepVoid && line.matches("^ *$")) {
                isVoidLine = true;
            } else {
                result.append(stepSeparator).append(line).append("\n");
            }
        }
        return result.toString().trim();
    }


    @Override
    public void update(AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        CaretModel caretModel = editor.getCaretModel();
        e.getPresentation().setEnabledAndVisible(caretModel.getCurrentCaret().hasSelection());
    }

}

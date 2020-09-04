package ru.lanit.ideaplugin.simplegit.popup;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

public class JBPopup {

    public JBPopup(AnActionEvent e, String message, MessageType messageType ) {
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, messageType, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(WindowManager.getInstance().getStatusBar(e.getProject()).getComponent()),
                        Balloon.Position.atRight);
    }


}

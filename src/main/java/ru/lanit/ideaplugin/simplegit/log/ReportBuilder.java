package ru.lanit.ideaplugin.simplegit.log;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import ru.lanit.ideaplugin.simplegit.SimpleGitProjectComponent;

public class ReportBuilder {
    public static final String GROUP_DISPLAY_ID = "groupDisplayId";
    public static final String TITLE = "title";
    public static final String CONTENT = "content";

    ReportBuilder() {
        SimpleGitProjectComponent.applicationInfo.getBuild().asString();
        SimpleGitProjectComponent.ideaPluginDescriptor.getVersion();
        SimpleGitProjectComponent.ideaPluginDescriptor.getSinceBuild();
        SimpleGitProjectComponent.ideaPluginDescriptor.getUntilBuild();
    }

    public static void notificate() {
        //groupId is important for further settings
        Notification notification = new Notification(GROUP_DISPLAY_ID, TITLE, CONTENT, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);//Notifications, not Notification
        notification.getBalloon().hide();
    }
}

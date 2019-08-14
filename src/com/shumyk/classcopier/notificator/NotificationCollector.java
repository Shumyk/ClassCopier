package com.shumyk.classcopier.notificator;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public class NotificationCollector {

    private static final String NL = "\n";

    private final Project project;

    private String groupDisplayId;
    private String title;
    private NotificationType type;

    private String mainMessage = "";
    private String additionalMessages = "";

    public NotificationCollector(String groupDisplayId, Project project, String title, NotificationType type) {
        this.groupDisplayId = groupDisplayId;
        this.project = project;
        this.title = title;
        this.type = type;
    }

    public void doNotify() {
        if (mainMessage.equals("") && additionalMessages.equals("")) return;

        String collectedMessage = mainMessage + NL + additionalMessages.substring(0, additionalMessages.length() - 2);
        Notification notification = new Notification(groupDisplayId, title, collectedMessage, type);
        notification.notify(project);
    }

    public void collect(final String mainMessage, final String additional) {
        if (!this.mainMessage.equals(mainMessage)) this.mainMessage = mainMessage;
        additionalMessages = additionalMessages + additional + "," + NL;
    }



}

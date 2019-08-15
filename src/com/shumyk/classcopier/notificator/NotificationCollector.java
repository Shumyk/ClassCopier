package com.shumyk.classcopier.notificator;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/**
 * Additional layer over Notification in order to have main message and several additional messages for single Notification.
 * Main message displayed firstly and then from new line additional messages shown.
 */
class NotificationCollector {

    private static final String NL = "\n";
    private static final String EMPTY = "";
    private static final String COMMA = ",";

    private final Project project;

    private String groupDisplayId;
    private String title;
    private NotificationType type;

    private String mainMessage = EMPTY;
    private String additionalMessages = EMPTY;

    /**
     * Initializes new instance of class for provided parameters.
     * @param groupDisplayId - ID that are shown in IDE
     * @param project - project where notification should be shown
     * @param title - title of the notification
     * @param type - warning, error, info
     */
    NotificationCollector(String groupDisplayId, Project project, String title, NotificationType type) {
        this.groupDisplayId = groupDisplayId;
        this.project = project;
        this.title = title;
        this.type = type;
    }

    /**
     * Collects messages.
     * Main message should be only one, when user can add several additional messages (they will be connected with comma and new line).
     * @param mainMessage - main message of content
     * @param additional - additional message to add to others.
     */
    void collect(final String mainMessage, final String additional) {
        if (!this.mainMessage.equals(mainMessage)) this.mainMessage = mainMessage;
        additionalMessages = additionalMessages + additional + COMMA + NL;
    }

    /**
     * Builds up together main message and additional ones.
     * From additional messages last two characters are cut (comma and new line).
     * Then show them in IDE.
     * Not going to execute if messages are empty.
     */
    void doNotify() {
        if (mainMessage.equals(EMPTY) && additionalMessages.equals(EMPTY)) return;

        String collectedMessage = mainMessage + NL + additionalMessages.substring(0, additionalMessages.length() - 2);
        Notification notification = new Notification(groupDisplayId, title, collectedMessage, type);
        notification.notify(project);
    }
}

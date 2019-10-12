package com.shumyk.classcopier.notificator;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/**
 * Class created for working with two notificators that are used in project.
 * In order to separate all management and work it was placed here.
 * Class creates new notificators, adds then new messages to them and displays notifications at the end.
 */
public class NotificationWorker {

    private final Project project;

    private static final String GROUP_ID_FFF = "ClassCopierFailedFindFiles";
    private static final String GROUP_ID_FCF = "ClassCopierFailedCopyFiles";
    private static final String GROUP_ID_NF = "ClassCopierNewFile";
    private static final String GROUP_ID_SE = "ClassCopierSuccessEnd";
    private static final String TITLE = "ClassCopier";
    private static final String TITLE_NF = "ClassCopier: Add new file to VSC";
    private static final String TITLE_SE = "ClassCopier: Success";

    private static final String MAIN_MESSAGE_FC = "Can't copy files.\nPlease check that files have not read-only status:";
    private static final String MAIN_MESSAGE_FF = "Apparently, output folder is empty.\nCouldn't find:\n";
    private static final String MAIN_MESSAGE_NF = "New files was added to VSC, so class files were copied to source root.\nPlease, add copied class files to VSC:\n";
    private static final String MAIN_MESSAGE_SE = "ClassCopier finished successfully, copied files should be shown in VSC window. \nIf not, please, try to refresh VSC window.";

    private NotificationCollector failedFindNotificator;
    private NotificationCollector failedCopyNotificator;
    private NotificationCollector newFileNotificator;

    /**
     * Inits both notificators for provided project.
     * All other parameters are default.
     * @param project - current project where notifications should appear.
     */
    public NotificationWorker(Project project) {
        this.project = project;
        failedFindNotificator = new NotificationCollector(GROUP_ID_FFF, project, TITLE, NotificationType.ERROR);
        failedCopyNotificator = new NotificationCollector(GROUP_ID_FCF, project, TITLE, NotificationType.ERROR);
        newFileNotificator = new NotificationCollector(GROUP_ID_NF, project, TITLE_NF, NotificationType.INFORMATION);
    }

    /**
     * Adds new additional message to Failed Copy notificator.
     * @param additionalMessage - new additional message
     */
    public void addFailedCopy(final String additionalMessage) {
        failedCopyNotificator.collect(MAIN_MESSAGE_FC, additionalMessage);
    }

    /**
     * Adds new additional message to Failed Found notificator.
     * @param additionalMessage - new additional message
     */
    public void addFailedFind(final String additionalMessage) {
        failedFindNotificator.collect(MAIN_MESSAGE_FF, additionalMessage);
    }

    /**
     * Adds new additional message to New File notificator.
     * @param additionalMessage - new additional message
     */
    public void addNewFile(final String additionalMessage) {
        newFileNotificator.collect(MAIN_MESSAGE_NF, additionalMessage);
    }

    /**
     * Show all notifications that were collected in project.
     */
    public void doNotify() {
        failedFindNotificator.doNotify();
        failedCopyNotificator.doNotify();
        newFileNotificator.doNotify();
    }

    public void notifySuccess() {
        Notification successNotification = new Notification(GROUP_ID_SE, TITLE_SE, MAIN_MESSAGE_SE, NotificationType.INFORMATION);
        successNotification.notify(project);
    }
}

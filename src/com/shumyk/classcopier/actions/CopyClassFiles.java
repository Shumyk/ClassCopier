package com.shumyk.classcopier.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.*;
import com.shumyk.classcopier.files.FilesWorker;
import com.shumyk.classcopier.module.ModuleWorker;
import com.shumyk.classcopier.notificator.NotificationWorker;
import com.shumyk.classcopier.paths.PathBusiness;

import java.util.List;

public class CopyClassFiles extends AnAction {

    /**
     * Action which performs search for changed files in VSC, determines if it's Java file and if yes searches those
     * compiled version in compiler output location. Found compiled classes is copied to location of changed files.
     * @param event - object with info about user's environment
     */
    @Override public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        NotificationWorker notificationWorker = new NotificationWorker(project);
        String compilerOut = ModuleWorker.getCompilerOutput(project);
        List<String> sourceRoots = ModuleWorker.getSourceRoots(project);

        ChangeListManager.getInstance(project).getDefaultChangeList().getChanges()
                .stream().filter(change -> isJavaFile(change, notificationWorker))
                .forEach(change -> copyClassFile(change, compilerOut, sourceRoots, notificationWorker));

        notificationWorker.doNotify();
        notificationWorker.notifySuccess();
    }

    /**
     * Creates all needed paths and then perform search for class file in output location.
     * Searches for provided changed file compiled class.
     * Then performs for every found file copy operation to source location.
     * @param change - wrapper over changed file
     * @param compilerOut - location of compiled classes
     * @param sourceRoots - source roots of project's modules
     * @param notificationWorker - NotificationWorker is passed here further for collecting error messages.
     */
    private void copyClassFile(final Change change, final String compilerOut,
                               final List<String> sourceRoots, final NotificationWorker notificationWorker) {
        if (change.getVirtualFile() == null) return;

        String fileUrl = change.getVirtualFile().getPath();
        String javaAbsoluteDirLocation = PathBusiness.cutFilename(fileUrl);
        String relativeJavaFileLocation = PathBusiness.getRelativeFileLocation(sourceRoots, fileUrl);
        String filePackage = PathBusiness.cutFilename(relativeJavaFileLocation);
        String filename = PathBusiness.getFilenameWithoutExtension(relativeJavaFileLocation);
        String compilerOutputUrl = compilerOut + filePackage;

        FilesWorker.findFiles(compilerOutputUrl, filename, notificationWorker)
                .forEach(file -> FilesWorker.copyFile(file, javaAbsoluteDirLocation, notificationWorker));
    }

    /**
     * Performs check is provided change is for Java file.
     * If file is Java class and it is new file it adds notification for user to not forget add it to VCS.
     * @param change - wrapper over changed file
     * @param notificationWorker - NotificationWorker used in case of situation when file is new and Java format in order to deliver remainder to user.
     * @return (false|true)
     */
    private boolean isJavaFile(Change change, final NotificationWorker notificationWorker) {
        if (change.getVirtualFile() == null) return false;

        String filePath = change.getVirtualFile().getPath();
        boolean isJavaFile = PathBusiness.endsWithJava(filePath);

        if (change.getFileStatus() == FileStatus.ADDED && isJavaFile)
            notificationWorker.addNewFile(PathBusiness.extensionToClass(filePath));

        return isJavaFile;
    }
}

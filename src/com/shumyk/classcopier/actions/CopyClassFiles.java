package com.shumyk.classcopier.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.*;
import com.shumyk.classcopier.FilesWorker;
import com.shumyk.classcopier.module.ModuleWorker;
import com.shumyk.classcopier.notificator.NotificationWorker;
import com.shumyk.classcopier.paths.PathBusiness;

import java.util.Collection;
import java.util.List;

public class CopyClassFiles extends AnAction {

    @Override public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        NotificationWorker notificationWorker = new NotificationWorker(project);
        String compilerOut = ModuleWorker.getCompilerOutput(project);
        List<String> sourceRoots = ModuleWorker.getSourceRoots(project);

        LocalChangeList localChangeList = ChangeListManager.getInstance(project).getDefaultChangeList();
        Collection<Change> changes = localChangeList.getChanges();

        changes.stream()
                .filter(el -> {
                    if (el.getVirtualFile() == null) return false;
                    String filePath = el.getVirtualFile().getPath();
                    boolean isJavaFile = PathBusiness.endsWithJava(filePath);
                    if (el.getFileStatus() == FileStatus.ADDED && isJavaFile)
                        notificationWorker.addNewFile(PathBusiness.extensionToClass(filePath));
                   return isJavaFile;
                })
                .forEach(el -> copyClassFile(el, compilerOut, sourceRoots, notificationWorker));

        notificationWorker.doNotify();
    }

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
}

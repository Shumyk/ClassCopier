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

import java.io.IOException;
import java.nio.file.*;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class CopyClassFiles extends AnAction {

    private NotificationWorker notificationWorker;

    private String compilerOut;
    private List<String> sourceRoots;

    @Override public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        notificationWorker = new NotificationWorker(project);
        compilerOut = ModuleWorker.getCompilerOutput(project);
        sourceRoots = ModuleWorker.getSourceRoots(project);

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
                .forEach(this::copyClassFile);

        notificationWorker.doNotify();
    }

    private void copyClassFile(final Change change) {
        if (change.getVirtualFile() == null) return;
        String fileUrl = change.getVirtualFile().getPath();

        final String javaAbsoluteDirLocation = PathBusiness.cutFilename(fileUrl);
        final String relativeJavaFileLocation = PathBusiness.getRelativeFileLocation(sourceRoots, fileUrl);
        final String filePackage = PathBusiness.cutFilename(relativeJavaFileLocation);
        final String filename = PathBusiness.getFilenameWithoutExtension(relativeJavaFileLocation);

        Stream<Path> files = null;
        String compilerOutputUrl = compilerOut + filePackage;
        try {
            Path compilerOutputPath = Paths.get(compilerOutputUrl);
            files = Files.find(compilerOutputPath, 50, (path, attributes) ->
                    path.toFile().getName().matches(PathBusiness.regexFilename(filename)) && PathBusiness.endsWithClass(path.toFile().getName()));

            files.forEach(file -> {
                String destinationUrl = javaAbsoluteDirLocation + file.toFile().getName();
                try {
                    FilesWorker.setFileWritable(destinationUrl);
                    Files.copy(file, Paths.get(destinationUrl), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ioe) {
                    notificationWorker.addFailedCopy(destinationUrl);
                }
            });
        } catch (IOException ioe) {
            notificationWorker.addFailedFind(compilerOutputUrl);
        } finally {
            if (files != null) files.close();
        }
    }
}

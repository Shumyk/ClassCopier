package com.shumyk.classcopier.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.*;
import com.shumyk.classcopier.FilesWorker;
import com.shumyk.classcopier.module.ModuleWorker;
import com.shumyk.classcopier.notificator.NotificationWorker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        notificationWorker = new NotificationWorker(project);
        compilerOut = ModuleWorker.getCompilerOutput(project);
        sourceRoots = ModuleWorker.getSourceRoots(project);

        LocalChangeList localChangeList = ChangeListManager.getInstance(project).getDefaultChangeList();
        Collection<Change> changes = localChangeList.getChanges();

        changes.stream()
                .filter(el -> {
                    String filePath = el.getVirtualFile().getPath();
                    boolean isJavaFile = filePath.endsWith(".java");
                    if (el.getFileStatus() == FileStatus.ADDED && isJavaFile)
                        notificationWorker.addNewFile(filePath.replaceFirst("\\.java$", ".class"));
                   return isJavaFile;
                })
                .forEach(this::copyClassFile);

        notificationWorker.doNotify();
    }

    private void copyClassFile(final Change change) {
        String fileUrl = change.getVirtualFile().getPath();

        final String javaAbsoluteDirLocation = fileUrl.replaceFirst("[a-zA-Z0-9_.-]+java$", "");
        final String javaFileLocation = getRelativeFileLocation(fileUrl);
        final String classFileDirDestination = javaFileLocation.replaceFirst("[a-zA-Z0-9_.-]+java$", "");
        final String classFilename = javaFileLocation
                .replaceFirst(".+/", "")
                .replaceFirst(".java$", "");

        Stream<Path> files = null;
        String compilerOutputUrl = compilerOut + classFileDirDestination;
        try {
            Path compilerOutputPath = Paths.get(compilerOutputUrl);
            files = Files.find(compilerOutputPath, 50, (path, attributes) -> {
                String filename = path.toFile().getName();
                return filename.matches("^" + classFilename + "(\\$.+\\.|\\.)class") && filename.endsWith(".class");
            });

            files.forEach(file -> {
                String destinationUrl = javaAbsoluteDirLocation + file.toFile().getName();
                try {
                    FilesWorker.setFileWritable(destinationUrl);

                    Path destination = Paths.get(destinationUrl);
                    Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
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

    /**
     * Walks through source roots that we have in this module and searches proper correct root for file URL.
     * If nothing found then return null.
     * File URL is cut with found file URL and returned relative path to file.
     * @param fileUrl - full path to a file
     * @return null - when nothing found, relative according to source root path.
     */
    @Nullable private String getRelativeFileLocation(@NotNull final String fileUrl) {
        String correctRoot = "";
        // searching for proper source root
        for (String root: sourceRoots) {
            correctRoot = fileUrl.contains(root) ? root : correctRoot;
        }

        if (correctRoot.equals("")) return null;
        // cutting all path with source root folder in order to make relative
        return fileUrl.replaceFirst(".+" + correctRoot, "");
    }
}

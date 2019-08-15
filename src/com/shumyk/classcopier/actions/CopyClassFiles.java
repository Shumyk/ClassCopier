package com.shumyk.classcopier.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.shumyk.classcopier.notificator.NotificationWorker;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class CopyClassFiles extends AnAction {

    private static final Logger LOG = Logger.getLogger(CopyClassFiles.class);

    private NotificationWorker notificationWorker;

    private String compilerOut;
    private List<String> sourceRoots;

    @Override public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        notificationWorker = new NotificationWorker(project);
        initCompilerOut(project);

        LocalChangeList localChangeList = ChangeListManager.getInstance(project).getDefaultChangeList();
        Collection<Change> changes = localChangeList.getChanges();

        changes.stream()
                .filter(el -> el.getBeforeRevision().getFile().getPath().endsWith(".java"))
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
                    setFileWritable(destinationUrl);

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

    private void setFileWritable(final String fileUrl) {
            Runnable writeAction = () -> {
                try {
                    VirtualFile file = VirtualFileManager.getInstance().findFileByUrl("file://".concat(fileUrl));
                    if (file != null) file.setWritable(true);
                } catch (IOException ioe) {
                    LOG.error("Error during setting file as writable.", ioe);
                }
            };
            ApplicationManager.getApplication().runWriteAction(writeAction);
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

    /**
     * Walks through all modules in provided project and searches module with sources root.
     * For this root then obtains compiler output folder path and sets to class variable.
     * @param project - current project
     */
    private void initCompilerOut(Project project) {
        Module moduleWithRoots = null;
        // search through all modules in project
        for (Module module: ModuleManager.getInstance(project).getModules()) {
            VirtualFile[] files = ModuleRootManager.getInstance(module).getSourceRoots();
            // if module has some sources root - we need to output folder for this root
            if (files.length > 0) {
                moduleWithRoots = module;

                sourceRoots = new ArrayList<>();
                for (VirtualFile file : files) {
                    sourceRoots.add("/" + file.getParent().getName() + "/" + file.getName());
                }
            }
        }

        // in case it didn't find anything try find default module 'src'
        if (moduleWithRoots == null)
            moduleWithRoots = ModuleManager.getInstance(project).findModuleByName("src");

        // obtain compiler output folder for module
        if (moduleWithRoots != null)
            compilerOut = CompilerPathsEx.getModuleOutputPath(moduleWithRoots, false);
    }
}

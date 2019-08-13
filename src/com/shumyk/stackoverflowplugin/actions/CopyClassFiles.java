package com.shumyk.stackoverflowplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collection;
import java.util.stream.Stream;

public class CopyClassFiles extends AnAction {

    private static final Logger LOG = Logger.getLogger(CopyClassFiles.class);

    private String compilerOut;

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        initCompilerOut(project);

        LocalChangeList localChangeList = ChangeListManager.getInstance(project).getDefaultChangeList();
        Collection<Change> changes = localChangeList.getChanges();

        changes.stream()
                .filter(el -> el.getBeforeRevision().getFile().getPath().endsWith(".java"))
                .forEach(this::copyClassFile);
    }

    private void copyClassFile(final Change change) {
        String fullFilename = change.getVirtualFile().getPath();

        final String javaAbsoluteDirLocation = fullFilename.replaceFirst("[a-zA-Z0-9_.-]+java$", "");
        final String javaFileLocation = fullFilename.replaceFirst(".+/src/Beans", ""); // TODO need to remove this hardcoding and somehow dynamically find src folder
        final String classFileDirDestination = javaFileLocation.replaceFirst("[a-zA-Z0-9_.-]+java$", "");
        final String classFilename = javaFileLocation
                .replaceFirst(".+/", "")
                .replaceFirst(".java$", "");

        Stream<Path> files = null;
        try {
            Path compilerOutputPath = Paths.get(compilerOut + classFileDirDestination);
            files = Files.find(compilerOutputPath, 50, (path, attributes) -> {
                String filename = path.toFile().getName();
                return filename.matches("^" + classFilename + "(\\$.+\\.|\\.)class") && filename.endsWith(".class");
            });

            files.forEach(file -> {
                try {
                    String destinationUrl = javaAbsoluteDirLocation + file.toFile().getName();
                    setFileWritable(destinationUrl);

                    Path destination = Paths.get(destinationUrl);
                    Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ioe) {
                    LOG.error("Error during copying of file.", ioe);
                }
            });
        } catch (IOException ioe) {
            LOG.error("Error during searching for modified files.", ioe);
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

    private void initCompilerOut(Project project) {
        if (compilerOut == null) {
            // TODO fix module tight up
            Module module = ModuleManager.getInstance(project).findModuleByName("src");
            compilerOut = CompilerPathsEx.getModuleOutputPath(module, false);
        }
    }
}

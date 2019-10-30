package com.shumyk.classcopier.module;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for dealing with Module business.
 */
public class ModuleWorker {

    private static final String SLASH = "/";
    private static final String DEFAULT_MODULE = "src";

    private ModuleWorker() {}

    /**
     * Walks through all modules in provided project and searches module with sources root.
     * For this root then obtains compiler output folder path and returns it.
     * @param project - current project
     */
    public static String getCompilerOutput(Project project) {
        Module moduleWithRoots = null;
        // search through all modules in project
        for (Module module: ModuleManager.getInstance(project).getModules()) {
            VirtualFile[] files = ModuleRootManager.getInstance(module).getSourceRoots();
            // if module has some sources root - we need to output folder for this root
            if (files.length > 0) moduleWithRoots = module;
        }

        // in case it didn't find anything try find default module 'src'
        if (moduleWithRoots == null)
            moduleWithRoots = ModuleManager.getInstance(project).findModuleByName(DEFAULT_MODULE);

        // obtain compiler output folder for module
        return CompilerPaths.getModuleOutputPath(moduleWithRoots, false);
    }

    /**
     * Walks through all modules in provided project and searches for source roots.
     * When finds it puts to list and then returns it.
     * @param project - where source roots should be searched
     * @return list with source roots.
     */
    public static List<String> getSourceRoots(Project project) {
        List<String> sourceRoots = null;
        for (Module module: ModuleManager.getInstance(project).getModules()) {
            VirtualFile[] files = ModuleRootManager.getInstance(module).getSourceRoots();
            if (files.length > 0) {
                sourceRoots = new ArrayList<>();
                for (VirtualFile file : files) {
                    sourceRoots.add(SLASH + file.getParent().getName() + SLASH + file.getName());
                }
            }
        }
        return sourceRoots;
    }
}

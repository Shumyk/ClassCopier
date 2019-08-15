package com.shumyk.classcopier.paths;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Different kind of business related to paths.
 */
public class PathBusiness {

    private static final String EMPTY = "";
    private static final String JAVA_EXT = ".java";
    private static final String CLASS_EXT = ".class";

    private static final String REGEX_START = "^";
    private static final String REGEX_END = "$";

    private PathBusiness() {}

    /**
     * Changes extension in filename from .java to .class.
     * @param filename - filename with extension
     * @return filename with .class extension.
     */
    public static String extensionToClass(final String filename) {
        return filename.replaceFirst("\\"+ JAVA_EXT + REGEX_END, CLASS_EXT);
    }

    /**
     * Substrings filename from path.
     * @param fileUrl - full filepath.
     * @return - filepath without filename (path to folder)
     */
    public static String cutFilename(final String fileUrl) {
        return fileUrl.replaceFirst("[a-zA-Z0-9_.-]+java$", EMPTY);
    }

    /**
     * Substrings all path to file, left is only filename without extension.
     * @param filepath - full path to file
     * @return - filename without extension
     */
    public static String getFilenameWithoutExtension(final String filepath) {
        return filepath.replaceFirst(".+/", EMPTY).replaceFirst(JAVA_EXT + REGEX_END, EMPTY);
    }

    /**
     * Walks through source roots that we have in this module and searches proper correct root for file URL.
     * If nothing found then return null.
     * File URL is cut with found file URL and returned relative path to file.
     * @param fileUrl - full path to a file
     * @return null - when nothing found, relative according to source root path.
     */
    @NotNull
    public static  String getRelativeFileLocation(@NotNull final List<String> sourceRoots, @NotNull final String fileUrl) {
        String correctRoot = EMPTY;
        // searching for proper source root
        for (String root: sourceRoots) {
            correctRoot = fileUrl.contains(root) ? root : correctRoot;
        }
        // cutting all path with source root folder in order to make relative
        return fileUrl.replaceFirst(".+" + correctRoot, EMPTY);
    }

    /**
     * Creates RegEx as string for filename.
     * This RegEx checks that another string starts with provided filename and ends with .class extension.
     * @param filename - of file
     * @return - string RegEx
     */
    public static String regexFilename(final String filename) {
        return REGEX_START + filename + "(\\$.+\\.|\\.)class";
    }

    /**
     * Checks if filename ends with .class
     * @param filename to be checked
     * @return (true|false)
     */
    public static boolean endsWithClass(final String filename) {
        return filename.endsWith(CLASS_EXT);
    }
    /**
     * Checks if filename ends with .java
     * @param filename to be checked
     * @return (true|false)
     */
    public static boolean endsWithJava(final String filename) {
        return filename.endsWith(JAVA_EXT);
    }
}

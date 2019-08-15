package com.shumyk.classcopier.files;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.shumyk.classcopier.notificator.NotificationWorker;
import com.shumyk.classcopier.paths.PathBusiness;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Class for dealing with Files business.
 */
public class FilesWorker {
    private static final Logger LOG = Logger.getLogger(FilesWorker.class);

    private static final String FILE_PROTOCOL = "file://";
    private static final int DEPTH = 50;

    private FilesWorker() {}

    /**
     * Searches for files on filesystem for provided output URL.
     * Performs check for filename against found item.
     * In case of exception puts new notification to NotificationWorker.     *
     * @param outputUrl - compiler output URL
     * @param filename - that is needed
     * @param notification - NotificationWorker used in case of exception in order to deliver error message to user.
     * @return (Stream<Path>) - to found files, if nothing found empty Stream in order to not break application.
     */
    public static Stream<Path> findFiles(final String outputUrl, final String filename, final NotificationWorker notification) {
        Stream<Path> files;
        Path compilerOutputPath = Paths.get(outputUrl);
        try {
            files = Files.find(compilerOutputPath, DEPTH, (path, attributes) -> isNeededFile(path.toFile().getName(), filename));
        } catch (IOException ioe) {
            notification.addFailedFind(outputUrl);
            files = Stream.empty();
        }
        return files;
    }
    /**
     * Performs check for found file against provided filename.
     * It should match the filename and end with .class extension.
     * @param foundFilename - filename of found file
     * @param neededFilename - filename of needed file
     * @return (true|false)
     */
    private static boolean isNeededFile(final String foundFilename, final String neededFilename) {
        return foundFilename.matches(PathBusiness.regexFilename(neededFilename)) && PathBusiness.endsWithClass(foundFilename);
    }

    /**
     * Copies file to destination URL. In need replaces existing files.
     * Sets needed file writable IntelliJ-wise before copying.
     * In case of exception puts new notification to NotificationWorker.
     * @param file - to be copied from
     * @param destinationDirUrl - destination directory URL
     * @param notification - NotificationWorker used in case of exception in order to deliver error message to user.
     */
    public static void copyFile(final Path file, final String destinationDirUrl, final NotificationWorker notification) {
        String destinationUrl = destinationDirUrl + file.toFile().getName();
        try {
            FilesWorker.setFileWritable(destinationUrl);
            Files.copy(file, Paths.get(destinationUrl), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            notification.addFailedCopy(destinationUrl);
        }
    }

    /**
     * Searches file by URL and then remove its read-only status and make it writable in IntelliJ.
     * @param fileUrl - URL to file which need to be writable.
     */
    private static void setFileWritable(final String fileUrl) {
        Runnable writeAction = () -> {
            try {
                VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(FILE_PROTOCOL.concat(fileUrl));
                if (file != null) file.setWritable(true);
            } catch (IOException ioe) {
                LOG.error("Error during setting file as writable.", ioe);
            }
        };
        ApplicationManager.getApplication().runWriteAction(writeAction);
    }
}

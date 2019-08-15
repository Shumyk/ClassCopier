package com.shumyk.classcopier;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Class for dealing with Files business.
 */
public class FilesWorker {
    private static final Logger LOG = Logger.getLogger(FilesWorker.class);

    private static final String FILE_PROTOCOL = "file://";

    private FilesWorker() {}

    /**
     * Searches file by URL and then remove its read-only status and make it writable in IntelliJ.
     * @param fileUrl - URL to file which need to be writable.
     */
    public static void setFileWritable(final String fileUrl) {
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

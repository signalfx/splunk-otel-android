package com.splunk.rum;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

// Basic wrapper around filesystem operations
public class FileUtils {

    void writeFileContents(File tmpFile, List<byte[]> blocksOfData) throws IOException {
        try (FileOutputStream out = new FileOutputStream(tmpFile)) {
            for (byte[] block : blocksOfData) {
                out.write(block);
            }
        }
    }

    byte[] readFileCompletely(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            int fileSize = (int) file.length();
            byte[] fileBuffer = new byte[fileSize];
            byte[] buffer = new byte[1024 * 10];
            int readSoFar = 0;
            while (readSoFar < fileSize) {
                int rc = in.read(buffer);
                System.arraycopy(buffer, 0, fileBuffer, readSoFar, rc);
                readSoFar += rc;
            }
            return fileBuffer;
        }
    }

    public File[] listFiles(File path, FileFilter fileFilter) {
        return path.listFiles(fileFilter);
    }
}

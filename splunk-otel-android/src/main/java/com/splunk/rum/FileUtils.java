package com.splunk.rum;

import static com.splunk.rum.SplunkRum.LOG_TAG;

import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Basic wrapper around filesystem operations, primarily for testing
public class FileUtils {

    void writeAsLines(Path tmpFile, List<byte[]> blocksOfData) throws IOException {
        try (FileOutputStream out = new FileOutputStream(tmpFile.toFile())) {
            for (byte[] block : blocksOfData) {
                out.write(block);
                out.write('\n');
            }
        }
    }

    List<byte[]> readFileCompletely(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String[] lines = new String(bytes).split("\n");
        return Arrays.stream(lines)
                .map(line -> line.getBytes(StandardCharsets.UTF_8))
                .collect(Collectors.toList());
    }

    Stream<Path> listFiles(Path dir) throws IOException {
        return Files.list(dir);
    }

    boolean isRegularFile(Path file){
        return Files.isRegularFile(file);
    }

    void safeDelete(Path file) {
        try {
            Files.delete(file);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Error deleting file " + file, e);
        }
    }

    Path moveAtomic(Path source, Path target) throws IOException {
        return Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    }

}

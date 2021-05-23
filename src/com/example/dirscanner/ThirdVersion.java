package com.example.dirscanner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class ThirdVersion {
    private final static String FILE_SEPARATOR = File.separator;

    public static void main(String[] args) {
        Path rootDirectory = Paths.get("C:" + FILE_SEPARATOR + "test" + FILE_SEPARATOR);
        Set<String> excludedDirectories = new HashSet<>();
        excludedDirectories.add("results");
        String resultFileDirectory = "C:" + FILE_SEPARATOR + "results";
        String resultFile = resultFileDirectory + FILE_SEPARATOR + "resultFile.txt";

        try(
                FileWriter fileWriter = new FileWriter(resultFile, true);
                BufferedWriter writer = new BufferedWriter(fileWriter)
        ) {
            process(writer, rootDirectory, excludedDirectories);
        } catch (IOException e) {
            throw new RuntimeException("failed to append data to result file: " + resultFile);
        }
    }

    public static void process(
            BufferedWriter writer, Path rootDirectory, Set<String> excludedDirectories
    ) {
        try (
                Stream<Path> stream = Files.list(rootDirectory)
        ) {
            stream
                    .parallel()
                    .filter(path -> !Files.isSymbolicLink(path))
                    .filter(path -> {
                        try {
                            return !Files.isHidden(path);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .filter(Files::isReadable)
                    .forEach(path -> {
                        if (Files.isDirectory(path) && !excludedDirectories.contains(path.getFileName().toString())) {
                            process(writer, path, excludedDirectories);
                        } else {
                            try {
                                writer
                                        .append(path.toAbsolutePath().toString())
                                        .append(System.lineSeparator());
                            } catch (IOException e) {
                                System.err.println(
                                        "failed to append data: " + path.toAbsolutePath() + " to temp result file"
                                );
                            }
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("failed to obtain subdirectories in the root directory: " + rootDirectory);
        }
    }
}

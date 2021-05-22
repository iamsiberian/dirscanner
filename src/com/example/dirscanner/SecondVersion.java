package com.example.dirscanner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public class SecondVersion {
    private final static String FILE_SEPARATOR = File.separator;

    public static void main(String[] args) {
        Path rootDirectory = Paths.get("C:" + FILE_SEPARATOR + "test" + FILE_SEPARATOR);
        Set<String> excludedDirectories = new HashSet<>();
        excludedDirectories.add("results");
        String resultFileDirectory = "C:" + FILE_SEPARATOR + "results";
        String resultFile = resultFileDirectory + FILE_SEPARATOR + "resultFile.txt";

        Set<DirPath> subDirectories = new TreeSet<>(
                Comparator.comparing(DirPath::directoryPath)
        );

        getSubdirectories(
                subDirectories, rootDirectory, excludedDirectories
        );

        subDirectories.add(new DirPath(rootDirectory.getFileName().toString(), rootDirectory.toAbsolutePath()));

        try(
                FileWriter fileWriter = new FileWriter(resultFile, true);
                BufferedWriter writer = new BufferedWriter(fileWriter)
        ) {
            subDirectories
                    .forEach(
                            subDirectory -> {
                                try {
                                    Stream<Path> filesInSubdirectory = Files.list(subDirectory.directoryPath());
                                    filesInSubdirectory
                                            .parallel()
                                            .filter(path -> !Files.isDirectory(path))
                                            .forEach(
                                                    path -> {
                                                        try {
                                                            writer
                                                                    .append(path.toAbsolutePath().toString())
                                                                    .append(System.lineSeparator());
                                                        } catch (IOException e) {
                                                            System.err.println(
                                                                    "failed to append data: " + path.toAbsolutePath() + " to temp result file: " + resultFile
                                                            );
                                                        }
                                                    }
                                            );
                                } catch (IOException e) {
                                    System.err.println("failed to obtain stream path of subdirectory: " + subDirectory);
                                }
                            }
                    );
        } catch (IOException e) {
            throw new RuntimeException("failed to append data to result file: " + resultFile);
        }
    }

    public static void getSubdirectories(
            Set<DirPath> subdirectories, Path rootDirectory, Set<String> excludedDirectories
    ) {
        try (Stream<Path> stream = Files.list(rootDirectory)) {
            stream.parallel()
                    .filter(Files::isDirectory)
                    .filter(path -> !excludedDirectories.contains(path.getFileName().toString()))
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
                        subdirectories.add(
                                new DirPath(path.getFileName().toString(), path.toAbsolutePath())
                        );
                        getSubdirectories(subdirectories, path, excludedDirectories);
                    });
        } catch (IOException e) {
            throw new RuntimeException("failed to obtain subdirectories in the root directory: " + rootDirectory);
        }
    }
}

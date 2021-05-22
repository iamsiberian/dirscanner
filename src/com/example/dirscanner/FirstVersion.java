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
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FirstVersion {
    private final static String FILE_SEPARATOR = File.separator;

    public static void main(String[] args) {
	/* todo implement
        Программа должна позволять исключать каталоги из анализа. Для этого нужно использовать ключ «минус»
        (-) и после него перечислить полные пути каталогов, которые анализу не подлежат.
                Ниже пример параметров, при которых ничего не сканируется:
        "\\epbyminsd0235\Video Materials" "\\EPUALVISA0002.kyiv.com\Workflow\ORG\Employees\Special"
        "\\EPUALVISA0002.kyiv.com\Workflow\ORG\Employees\Lviv" - "\\epbyminsd0235\Video Materials"
        "\\EPUALVISA0002.kyiv.com\Workflow\ORG\Employees\Special" "\\EPUALVISA0002.kyiv.com\Workflow\ORG\Employees\Lviv"
    */
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

        Set<String> tempResultFilePaths = readFilesFromSubdirectoriesAndGetPathsToTempResultFiles(
                subDirectories, resultFileDirectory
        );

        Optional<Stream<String>> resultStream = mergeStreamsOfPathToTempResultFiles(
                tempResultFilePaths
        );

        if (resultStream.isPresent()) {
            try(
                    FileWriter fileWriter = new FileWriter(resultFile, true);
                    BufferedWriter writer = new BufferedWriter(fileWriter)
            ) {
                resultStream
                        .get()
                        .parallel()
                        .forEach(fileData -> {
                            try {
                                writer.append(fileData).append(System.lineSeparator());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

            } catch (IOException e) {
                throw new RuntimeException("failed to append data to result file: " + resultFile);
            }
        } else {
            throw new RuntimeException("nothing to merge :(");
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

    public static Set<String> readFilesFromSubdirectoriesAndGetPathsToTempResultFiles(
            Set<DirPath> subdirectories, String resultFilesDirectory
    ) {
        return subdirectories
                .parallelStream()
                .map(subdirectory -> {
                    try {
                        Stream<Path> filesInSubdirectory = Files.list(subdirectory.directoryPath());
                        String tempResultFile = resultFilesDirectory + FILE_SEPARATOR + subdirectory.directoryName();
                        filesInSubdirectory
                                .parallel()
                                .filter(path -> !Files.isDirectory(path))
                                .forEach(path -> {
                                    try(
                                            FileWriter fileWriter = new FileWriter(tempResultFile, true);
                                            BufferedWriter writer = new BufferedWriter(fileWriter)
                                    ) {
                                        writer
                                                .append(path.toAbsolutePath().toString())
                                                .append(System.lineSeparator());
                                    } catch (IOException e) {
                                        throw new RuntimeException("failed to append data to temp result file: " + tempResultFile);
                                    }
                                });

                        return tempResultFile;
                    } catch (IOException e) {
                        throw new RuntimeException("failed to obtain stream path of subdirectory: " + subdirectory);
                    }
                })
                .collect(Collectors.toSet());
    }

    public static Optional<Stream<String>> mergeStreamsOfPathToTempResultFiles(
            Set<String> pathToTempResultFiles
    ) {
        return pathToTempResultFiles.parallelStream()
                .map(tempResultFile -> {
                    try {
                        return Files.lines(Paths.get(tempResultFile));
                    } catch (IOException e) {
                        throw new RuntimeException("failed to obtain path of temp file: " + tempResultFile);
                    }
                })
                .reduce(Stream::concat);
    }
}

package com.example.dirscanner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ThirdVersion {
    private final static String FILE_SEPARATOR = File.separator;
    private final static String EXCLUDE_DELIMITER = "-";
    private final static String RESULT_DIRECTORY_NAME = "results";
    private final static String RESULT_FILE_NAME = "resultFile.txt";

    public static void main(String[] args) {
        final ExcludedAndScannedDirectories excludedAndScannedDirectories = getExcludedAndScannedDirectories(args);
        for (String rootDirectory : excludedAndScannedDirectories.scannedDirectories()) {
            prepareProcessing(rootDirectory, excludedAndScannedDirectories.excludedDirectories());
        }
    }

    public static ExcludedAndScannedDirectories getExcludedAndScannedDirectories(String[] args) {
        if (args.length == 0) {
            throw new RuntimeException("Usage: java -jar *.jar inputDir1 inputDir2 - excludeDir1 excludeDir2");
        }
        Set<String> excludedDirectories = new HashSet<>();
        final int delimiterPosition = findDelimiterPosition(args);
        final List<String> argsList = Arrays.asList(args);
        if (delimiterPosition != 0 && delimiterPosition + 1 < args.length) {
            excludedDirectories.addAll(argsList.subList(delimiterPosition + 1, args.length));
        }
        Set<String> scannedDirectories = new HashSet<>(argsList.subList(0, delimiterPosition));
        return new ExcludedAndScannedDirectories(
                scannedDirectories, excludedDirectories
        );
    }

    public static int findDelimiterPosition(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (EXCLUDE_DELIMITER.equals(args[i])) {
                return i;
            }
        }
        return 0;
    }

    public static void prepareProcessing(String rootDirectory, Set<String> excludedDirectories) {
        Path rootSearchDirectory = Paths.get(rootDirectory + FILE_SEPARATOR);
        String resultFileDirectory = createResultFileDirectoryIfNotExist(rootDirectory + FILE_SEPARATOR + RESULT_DIRECTORY_NAME);
        String resultFile = resultFileDirectory + FILE_SEPARATOR + RESULT_FILE_NAME;
        excludedDirectories.add(resultFileDirectory);

        try(
                FileWriter fileWriter = new FileWriter(resultFile, true);
                BufferedWriter writer = new BufferedWriter(fileWriter)
        ) {
            process(writer, rootSearchDirectory, excludedDirectories);
        } catch (IOException e) {
            throw new RuntimeException("failed to append data to result file: " + resultFile);
        }
    }

    public static String createResultFileDirectoryIfNotExist(String resultFileDirectory) {
        Path resultFileDirectoryPath = Paths.get(resultFileDirectory);
        if (!Files.exists(resultFileDirectoryPath)) {
            try {
                Files.createDirectory(resultFileDirectoryPath);
            } catch (IOException e) {
                throw new RuntimeException("failed to create result file directory: " + resultFileDirectory);
            }
        }
        return resultFileDirectory;
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
                    .filter(path -> !excludedDirectories.contains(path.toAbsolutePath().toString()))
                    .forEach(path -> {
                        if (Files.isDirectory(path)) {
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

package com.example.dirscanner;

import java.util.Set;

public record ExcludedAndScannedDirectories(
        Set<String> scannedDirectories,
        Set<String> excludedDirectories
) {
}

package io.github.zoomshields16.linkchecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Command line tool that checks every link in a file and prints a report. */
public final class LinkChecker {

    static List<String> readUrls(Path file) throws IOException {
        return Files.readAllLines(file).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
    }
}

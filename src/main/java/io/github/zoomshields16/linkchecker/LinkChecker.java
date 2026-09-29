package io.github.zoomshields16.linkchecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Command line tool that checks every link in a file and prints a report. */
public final class LinkChecker {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String SEQUENTIAL_FLAG = "--sequential";

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        List<String> argList = List.of(args);
        boolean sequential = argList.contains(SEQUENTIAL_FLAG);
        List<String> files = argList.stream().filter(arg -> !arg.equals(SEQUENTIAL_FLAG)).toList();
        if (files.size() != 1) {
            System.err.println("Usage: java -jar link-checker.jar [--sequential] <url-file>");
            return 2;
        }

        List<String> urls;
        try {
            urls = readUrls(Path.of(files.getFirst()));
        } catch (IOException e) {
            System.err.println("Could not read " + files.getFirst());
            return 2;
        }

        long start = System.nanoTime();
        List<Checker.Result> results = new Checker(TIMEOUT).checkAll(urls, sequential);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        for (Checker.Result result : results) {
            String code = result.code() == 0 ? "" : String.valueOf(result.code());
            System.out.printf("%-8s  %3s  %s%n", result.status(), code, result.url());
        }
        System.out.println();
        System.out.println(summary(results, elapsed));

        long failed = count(results, Status.BROKEN) + count(results, Status.TIMEOUT);
        return failed > 0 ? 1 : 0;
    }

    static List<String> readUrls(Path file) throws IOException {
        return Files.readAllLines(file).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    static String summary(List<Checker.Result> results, Duration elapsed) {
        return "%d links: %d ok, %d redirect, %d broken, %d timed out in %.2fs".formatted(
                results.size(),
                count(results, Status.OK),
                count(results, Status.REDIRECT),
                count(results, Status.BROKEN),
                count(results, Status.TIMEOUT),
                elapsed.toMillis() / 1000.0);
    }

    private static long count(List<Checker.Result> results, Status status) {
        return results.stream().filter(result -> result.status() == status).count();
    }
}

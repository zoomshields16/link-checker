package io.github.zoomshields16.linkchecker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests reading the URL file, the summary line and the exit codes. */
class LinkCheckerTest {

    @TempDir
    Path dir;

    @Test
    void readsUrlsSkippingBlankLines() throws IOException {
        Path file = write("https://a.test\r\n\n   https://b.test  \n\t\n");

        assertEquals(List.of("https://a.test", "https://b.test"), LinkChecker.readUrls(file));
    }

    @Test
    void summaryCountsEachStatus() {
        List<Checker.Result> results = List.of(
                new Checker.Result("a", Status.OK, 200),
                new Checker.Result("b", Status.OK, 200),
                new Checker.Result("c", Status.REDIRECT, 301),
                new Checker.Result("d", Status.TIMEOUT, 0));

        assertEquals("4 links: 2 ok, 1 redirect, 0 broken, 1 timed out in 1.25s",
                LinkChecker.summary(results, Duration.ofMillis(1250)));
    }

    @Test
    void exitsZeroWhenNothingFails() throws IOException {
        assertEquals(0, LinkChecker.run(new String[] {write("").toString()}));
    }

    @Test
    void exitsOneWhenALinkIsBroken() throws IOException {
        assertEquals(1, LinkChecker.run(new String[] {"--sequential", write("not a url").toString()}));
    }

    @Test
    void exitsTwoOnBadUsage() {
        assertEquals(2, LinkChecker.run(new String[] {}));
        assertEquals(2, LinkChecker.run(new String[] {dir.resolve("missing.txt").toString()}));
    }

    private Path write(String content) throws IOException {
        return Files.writeString(dir.resolve("urls.txt"), content);
    }
}

package io.github.zoomshields16.linkchecker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests reading the URL file. */
class LinkCheckerTest {

    @Test
    void readsUrlsSkippingBlankLines(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("urls.txt");
        Files.writeString(file, "https://a.test\r\n\n   https://b.test  \n\t\n");

        assertEquals(List.of("https://a.test", "https://b.test"), LinkChecker.readUrls(file));
    }
}

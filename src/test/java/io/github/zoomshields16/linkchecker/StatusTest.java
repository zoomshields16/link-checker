package io.github.zoomshields16.linkchecker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests how status codes map to a status. */
class StatusTest {

    @ParameterizedTest
    @CsvSource({"200, OK", "299, OK", "301, REDIRECT", "399, REDIRECT", "404, BROKEN", "503, BROKEN"})
    void mapsCodeToStatus(int code, Status expected) {
        assertEquals(expected, Status.fromCode(code));
    }
}

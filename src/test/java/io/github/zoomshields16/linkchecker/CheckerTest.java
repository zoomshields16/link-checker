package io.github.zoomshields16.linkchecker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests the checker against a local server, so no real sites are needed. */
class CheckerTest {

    private static final Checker CHECKER = new Checker(Duration.ofMillis(500));
    private static HttpServer server;
    private static String base;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> respond(exchange, 200));
        server.createContext("/moved", exchange -> respond(exchange, 301));
        server.createContext("/missing", exchange -> respond(exchange, 404));
        server.createContext("/slow", exchange -> {
            sleep(Duration.ofSeconds(2));
            respond(exchange, 200);
        });
        // The default executor serves one request at a time, so the slow page would block the rest.
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @ParameterizedTest
    @CsvSource({"/ok, OK, 200", "/moved, REDIRECT, 301", "/missing, BROKEN, 404"})
    void reportsStatusFromServer(String path, Status status, int code) {
        assertEquals(new Checker.Result(base + path, status, code), CHECKER.check(base + path));
    }

    @Test
    void slowPageTimesOut() {
        assertEquals(Status.TIMEOUT, CHECKER.check(base + "/slow").status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not a url", "ftp://127.0.0.1/file", "http://127.0.0.1:1"})
    void unreachableUrlIsBroken(String url) {
        assertEquals(Status.BROKEN, CHECKER.check(url).status());
    }

    private static void respond(HttpExchange exchange, int code) throws IOException {
        exchange.sendResponseHeaders(code, -1);
        exchange.close();
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

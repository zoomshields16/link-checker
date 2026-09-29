package io.github.zoomshields16.linkchecker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/** Checks links over HTTP with a time limit on each request. */
final class Checker {

    record Result(String url, Status status, int code) {}

    private static final int MAX_IN_FLIGHT = 100;

    private final HttpClient client;
    private final Duration timeout;

    Checker(Duration timeout) {
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    List<Result> checkAll(List<String> urls, boolean sequential) {
        // Threads are cheap but sockets are not, so cap how many requests are open at once.
        Semaphore permits = new Semaphore(sequential ? 1 : MAX_IN_FLIGHT);
        List<Future<Result>> futures;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            futures = urls.stream().map(url -> executor.submit(() -> checkWithPermit(url, permits))).toList();
        }
        // Closing the executor waits for every task, so each result is ready here.
        return futures.stream().map(Future::resultNow).toList();
    }

    private Result checkWithPermit(String url, Semaphore permits) throws InterruptedException {
        permits.acquire();
        try {
            return check(url);
        } finally {
            permits.release();
        }
    }

    Result check(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            // Only the status code matters, and the timeout stops at the headers, so skip the body.
            response.body().close();
            return new Result(url, Status.fromCode(response.statusCode()), response.statusCode());
        } catch (HttpTimeoutException e) {
            return new Result(url, Status.TIMEOUT, 0);
        } catch (IOException | IllegalArgumentException e) {
            return new Result(url, Status.BROKEN, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(url, Status.BROKEN, 0);
        }
    }
}

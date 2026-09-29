package io.github.zoomshields16.linkchecker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/** Checks links over HTTP with a time limit on each request. */
final class Checker {

    record Result(String url, Status status, int code) {}

    private final HttpClient client;
    private final Duration timeout;

    Checker(Duration timeout) {
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
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

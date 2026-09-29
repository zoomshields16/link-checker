# link-checker
[![CI](https://github.com/zoomshields16/link-checker/actions/workflows/ci.yml/badge.svg)](https://github.com/zoomshields16/link-checker/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/java-21-blue)
[![License: MIT](https://img.shields.io/badge/license-MIT-green)](LICENSE)

A command line tool that reads a file of URLs, checks them all at the same time, and reports
which ones are OK, redirected, broken or timed out.

## Why Java
Java 21 added virtual threads. They are cheap enough to give every link its own thread, so the
checker is plain blocking code: send a request, wait, read the status. There's no async code or
callbacks, and the JDK's built-in `HttpClient` means there are no dependencies outside of JUnit.

## How it works
- Every link gets a virtual thread. At most 100 requests are open at once, since threads are
  cheap but network connections are not.
- Each request has a 5 second limit, so one slow site can't hang the run.
- Redirects are reported, not followed. A `301` shows up as `REDIRECT`.
- The exit code is 1 if any link is broken or timed out, so it can fail a CI job. Bad usage
  exits with 2.
- `--sequential` checks the same links one at a time, for comparing speed.

## Speed
Checking 100 popular sites, 10 runs of each mode from a laptop:

| Mode | Median |
|---|---|
| All at once | 1.1s |
| One at a time (`--sequential`) | 15.6s |

A run is only as fast as its slowest site. In 3 of the 10 runs one site was slow to answer
and hit the 5 second limit, so those runs took 5.2s.

## Build and run
You need JDK 21 or newer. The Maven Wrapper downloads Maven the first time, so there's nothing
else to install.

```
./mvnw package
java -jar target/link-checker.jar urls.txt
java -jar target/link-checker.jar --sequential urls.txt
```

Run the tests with `./mvnw test`. They use a local server from the JDK, so they don't need
the internet.

## Sample output
Running it on the included `urls.txt`:

```
OK        200  https://github.com
OK        200  https://openjdk.org
OK        200  https://maven.apache.org
OK        200  https://junit.org
OK        200  https://www.python.org
OK        200  https://www.wikipedia.org
OK        200  https://news.ycombinator.com
OK        200  https://www.mozilla.org
REDIRECT  301  http://github.com
BROKEN    404  https://github.com/zoomshields16/no-such-repo
BROKEN         https://no-such-site.invalid
TIMEOUT        http://example.com:81

12 links: 8 ok, 1 redirect, 2 broken, 1 timed out in 5.13s
```

The last link never answers, so this run takes as long as the 5 second limit.

## License
MIT

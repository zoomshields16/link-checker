package io.github.zoomshields16.linkchecker;

/** The outcome of checking one link. */
enum Status {
    OK, REDIRECT, BROKEN, TIMEOUT;

    static Status fromCode(int code) {
        return switch (code / 100) {
            case 2 -> OK;
            case 3 -> REDIRECT;
            default -> BROKEN;
        };
    }
}

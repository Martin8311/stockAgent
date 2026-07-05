package com.harnessagent.web;

import org.springframework.http.HttpStatusCode;

public class ApiRequestException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public ApiRequestException(HttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatusCode statusCode() {
        return statusCode;
    }
}


package com.skybooker.auth.exception;

public class TokenException extends RuntimeException {
    public TokenException(String message) {
        super(message);
    }
}
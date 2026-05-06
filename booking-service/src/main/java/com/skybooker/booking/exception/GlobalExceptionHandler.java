package com.skybooker.booking.exception;

import com.skybooker.booking.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse notFound(Exception ex, HttpServletRequest req) {
        return new ApiErrorResponse(LocalDateTime.now(), 404, "Not Found", ex.getMessage(), req.getRequestURI(), List.of());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse bad(Exception ex, HttpServletRequest req) {
        return new ApiErrorResponse(LocalDateTime.now(), 400, "Bad Request", ex.getMessage(), req.getRequestURI(), List.of());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse generic(Exception ex, HttpServletRequest req) {
        return new ApiErrorResponse(LocalDateTime.now(), 500, "Error", ex.getMessage(), req.getRequestURI(), List.of());
    }
}
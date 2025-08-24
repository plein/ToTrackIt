package com.totrackit.controller;

import com.totrackit.dto.ErrorResponse;
import com.totrackit.dto.ValidationError;
import com.totrackit.exception.ProcessAlreadyCompletedException;
import com.totrackit.exception.ProcessAlreadyExistsException;
import com.totrackit.exception.ProcessNotFoundException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Error;

import io.micronaut.http.annotation.Produces;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Requires(classes = {ProcessAlreadyExistsException.class, ProcessNotFoundException.class})
public class GlobalExceptionHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Error(exception = ProcessAlreadyExistsException.class)
    @Produces
    public HttpResponse<ErrorResponse> handleProcessAlreadyExists(HttpRequest<?> request, ProcessAlreadyExistsException ex) {
        LOG.warn("Process already exists: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                "PROCESS_ALREADY_EXISTS",
                ex.getMessage(),
                request.getPath()
        );
        
        return HttpResponse.status(HttpStatus.CONFLICT).body(error);
    }
    
    @Error(exception = ProcessNotFoundException.class)
    @Produces
    public HttpResponse<ErrorResponse> handleProcessNotFound(HttpRequest<?> request, ProcessNotFoundException ex) {
        LOG.warn("Process not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                "PROCESS_NOT_FOUND",
                ex.getMessage(),
                request.getPath()
        );
        
        return HttpResponse.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @Error(exception = ProcessAlreadyCompletedException.class)
    @Produces
    public HttpResponse<ErrorResponse> handleProcessAlreadyCompleted(HttpRequest<?> request, ProcessAlreadyCompletedException ex) {
        LOG.warn("Process already completed: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                "PROCESS_ALREADY_COMPLETED",
                ex.getMessage(),
                request.getPath()
        );
        
        return HttpResponse.status(HttpStatus.CONFLICT).body(error);
    }
    
    @Error(exception = ConstraintViolationException.class)
    @Produces
    public HttpResponse<ErrorResponse> handleValidationErrors(HttpRequest<?> request, ConstraintViolationException ex) {
        LOG.warn("Validation error: {}", ex.getMessage());
        
        List<ValidationError> validationErrors = ex.getConstraintViolations()
                .stream()
                .map(this::mapConstraintViolation)
                .collect(Collectors.toList());
        
        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getPath(),
                validationErrors
        );
        
        return HttpResponse.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @Error(exception = IllegalArgumentException.class)
    @Produces
    public HttpResponse<ErrorResponse> handleIllegalArgument(HttpRequest<?> request, IllegalArgumentException ex) {
        LOG.warn("Invalid argument: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                request.getPath()
        );
        
        return HttpResponse.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @Error(exception = Exception.class)
    @Produces
    public HttpResponse<ErrorResponse> handleGenericError(HttpRequest<?> request, Exception ex) {
        LOG.error("Unexpected error occurred", ex);
        
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                request.getPath()
        );
        
        return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    private ValidationError mapConstraintViolation(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath().toString();
        String message = violation.getMessage();
        Object rejectedValue = violation.getInvalidValue();
        
        return new ValidationError(field, message, rejectedValue);
    }
}
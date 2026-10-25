package com.eventpass.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.password.CompromisedPasswordException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.AuthenticationException;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Slf4j 
@RestControllerAdvice 
public class GlobalExceptionHandler 
{
     @ExceptionHandler(ResourceNotFoundException.class)
     public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
          ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
          problem.setTitle("Resource Not Found");
          return problem;
     }

     @ExceptionHandler(DuplicateEmailException.class)
     public ProblemDetail handleDuplicateEmail(DuplicateEmailException ex) {
          ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
          problem.setTitle("Duplicate Email");
          return problem;
     }

     @ExceptionHandler(CompromisedPasswordException.class)
     public ProblemDetail handleCompromisedPassword(CompromisedPasswordException ex) {
          ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
          problem.setTitle("Compromised Password");
          return problem;
     }

     @ExceptionHandler(AuthenticationException.class)
     public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
          ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
          problem.setTitle("Authentication Failed");
          return problem;
     }

     @ExceptionHandler(Exception.class)
     public ProblemDetail handleGenericException(Exception ex) {
          ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occured");
          problem.setTitle("Internal Server Error");
          return problem;

     }

     @ExceptionHandler(MethodArgumentNotValidException.class)
     public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex){
          ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");
          problem.setTitle("Validation Error");
          Map<String, String> errors = new LinkedHashMap<>();
          for(FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
               errors.put(fieldError.getField(), fieldError.getDefaultMessage());
          }
          problem.setProperty("errors", errors);
          return problem;
     }

     // @ExceptionHandler(Exception.class)
     // public ProblemDetail handleGenericException(Exception ex) {
     //      log.error("Unhandled exception occurred", ex);

     //      ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
     //      problem.setTitle("Internal Server Error");
     //      return problem;
     // }
}

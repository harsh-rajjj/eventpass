package com.eventpass.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice 
public class GlobalExceptionHandler 
{
     @ExceptionHandler(ResourceNotFoundException.class)
     public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
          ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
          problem.setTitle("Resource Not Found");
          return problem;
     }

     @ExceptionHandler(Exception.class)
     public ProblemDetail handleGenericException(Exception ex) {
          ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occured");
          problem.setTitle("Internal Server Error");
          return problem;

     }

}

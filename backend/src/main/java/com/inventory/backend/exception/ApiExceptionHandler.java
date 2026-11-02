package com.inventory.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handle(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }

    // The database's own message names tables and columns: it goes to the server log, never to the browser
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handle(DataIntegrityViolationException e) {
        log.warn("Rejected by the database: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Datos no validos: revisa que ningun texto sea demasiado largo"));
    }
}

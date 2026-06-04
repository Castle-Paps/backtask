package org.example.registrotareas.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── 404 ──────────────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("NOT FOUND [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(crearRespuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    // ─── 400 Negocio ──────────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("BAD REQUEST [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(crearRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }

    // ─── 400 Validación de campos (@Valid) ────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Valor inválido",
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        log.warn("VALIDATION [{} {}]: {}", request.getMethod(), request.getRequestURI(), errores);
        Map<String, Object> body = crearRespuesta(HttpStatus.BAD_REQUEST, "Error de validación", request);
        body.put("errores", errores);
        return ResponseEntity.badRequest().body(body);
    }

    // ─── 400 JSON malformado ──────────────────────────────────────────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("MALFORMED JSON [{} {}]", request.getMethod(), request.getRequestURI());
        return ResponseEntity.badRequest()
                .body(crearRespuesta(HttpStatus.BAD_REQUEST, "JSON malformado en el cuerpo de la solicitud", request));
    }

    // ─── 400 Tipo de parámetro incorrecto ─────────────────────────────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String msg = "Parámetro '" + ex.getName() + "' tiene un tipo inválido";
        log.warn("TYPE MISMATCH [{} {}]: {}", request.getMethod(), request.getRequestURI(), msg);
        return ResponseEntity.badRequest()
                .body(crearRespuesta(HttpStatus.BAD_REQUEST, msg, request));
    }

    // ─── 500 Error interno ────────────────────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        log.error("INTERNAL ERROR [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(crearRespuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error interno del servidor. Inténtalo de nuevo.", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("UNHANDLED EXCEPTION [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(crearRespuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Ocurrió un error inesperado", request));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Map<String, Object> crearRespuesta(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return body;
    }
}

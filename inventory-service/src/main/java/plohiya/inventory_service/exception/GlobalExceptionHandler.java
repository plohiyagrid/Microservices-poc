package plohiya.inventory_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInventoryNotFoundException(InventoryNotFoundException ex) {
        log.error("Inventory not found: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("INVENTORY_NOT_FOUND")
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ProductOutOfStockException.class)
    public ResponseEntity<ErrorResponse> handleProductOutOfStockException(ProductOutOfStockException ex) {
        log.error("Product out of stock: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("PRODUCT_OUT_OF_STOCK")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InvalidInventoryRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInventoryRequestException(InvalidInventoryRequestException ex) {
        log.error("Invalid inventory request: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("INVALID_REQUEST")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Inventory data integrity violation. Please check your request.")
                .error("DATA_INTEGRITY_VIOLATION")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKeyException(DuplicateKeyException ex) {
        log.error("Duplicate key violation: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Inventory with this SKU code already exists.")
                .error("DUPLICATE_INVENTORY")
                .status(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex) {
        log.error("Data access exception: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Unable to access database. Please try again later.")
                .error("DATABASE_ACCESS_ERROR")
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQLException(SQLException ex) {
        log.error("SQL exception: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Database error occurred: " + ex.getMessage())
                .error("SQL_ERROR")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(errorMessage)
                .error("VALIDATION_ERROR")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("INVALID_REQUEST")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("An unexpected error occurred")
                .error("INTERNAL_SERVER_ERROR")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
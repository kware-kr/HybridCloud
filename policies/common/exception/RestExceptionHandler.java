package kware.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ValidationError> handleValidationException(BindException ex) {
        return getValidationErrorResponseEntity(ex.getBindingResult());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> requestBodyhandleValidationException(MethodArgumentNotValidException ex) {
        return getValidationErrorResponseEntity(ex.getBindingResult());
    }

    private ResponseEntity<ValidationError> getValidationErrorResponseEntity(BindingResult bindingResult) {
        ValidationError error = new ValidationError();
        error.setMessage("Validation failed");
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            error.addError(fieldError.getObjectName(), fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(error);
    }
}

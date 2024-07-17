package kware.common.exception;

import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;

public class ValidationError  {
    private String message;
    private List<FieldError> errors;

    public ValidationError() {
    }

    public ValidationError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public void setErrors(List<FieldError> errors) {
        this.errors = errors;
    }

    public void addError(String objectName, String field, String message) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(new FieldError(objectName, field, message));
    }
}

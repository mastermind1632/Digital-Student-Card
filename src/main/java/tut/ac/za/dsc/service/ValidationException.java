package tut.ac.za.dsc.service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Submitted data is not acceptable. Field names map to the messages shown next to each field. */
public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super(errors.isEmpty() ? "Invalid input" : errors.values().iterator().next());
        this.errors = new LinkedHashMap<>(errors);
    }

    public static ValidationException of(String field, String message) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(field, message);
        return new ValidationException(m);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}

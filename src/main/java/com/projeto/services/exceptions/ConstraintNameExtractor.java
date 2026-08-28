package com.projeto.services.exceptions;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

@Component
public class ConstraintNameExtractor {

    public Optional<String> extractConstraintName(Throwable exception) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = exception;

        while (current != null && visited.add(current)) {
            if (current instanceof ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();
                if (constraintName == null || constraintName.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(constraintName);
            }
            current = current.getCause();
        }

        return Optional.empty();
    }
}

package ru.donskikh.incidenthub.alert.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AlertBatchSizeValidator.class)
public @interface ValidAlertBatchSize {
    String message() default "Alert batch exceeds the configured maximum size";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

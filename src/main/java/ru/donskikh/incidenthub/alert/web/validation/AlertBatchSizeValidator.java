package ru.donskikh.incidenthub.alert.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.donskikh.incidenthub.alert.AlertProperties;

import java.util.List;

public class AlertBatchSizeValidator implements ConstraintValidator<ValidAlertBatchSize, List<?>> {
    private final AlertProperties properties;

    public AlertBatchSizeValidator(AlertProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(List<?> value, ConstraintValidatorContext context) {
        return value == null || value.size() <= properties.maxBatchSize();
    }
}

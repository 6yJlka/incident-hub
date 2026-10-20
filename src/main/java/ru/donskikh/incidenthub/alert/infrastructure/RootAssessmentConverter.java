package ru.donskikh.incidenthub.alert.infrastructure;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.donskikh.incidenthub.alert.correlation.RootAssessment;

@Converter
public class RootAssessmentConverter implements AttributeConverter<RootAssessment, String> {
    @Override
    public String convertToDatabaseColumn(RootAssessment assessment) {
        return assessment == null ? null : CorrelationJsonCodec.writeAssessment(assessment);
    }

    @Override
    public RootAssessment convertToEntityAttribute(String json) {
        return json == null ? null : CorrelationJsonCodec.readAssessment(json);
    }
}

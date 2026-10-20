package ru.donskikh.incidenthub.alert.infrastructure;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.donskikh.incidenthub.alert.correlation.Evidence;

import java.util.List;

@Converter
public class LinkEvidenceConverter implements AttributeConverter<List<Evidence>, String> {
    @Override
    public String convertToDatabaseColumn(List<Evidence> evidence) {
        return evidence == null ? null : CorrelationJsonCodec.writeEvidence(evidence);
    }

    @Override
    public List<Evidence> convertToEntityAttribute(String json) {
        return json == null ? null : CorrelationJsonCodec.readEvidence(json);
    }
}

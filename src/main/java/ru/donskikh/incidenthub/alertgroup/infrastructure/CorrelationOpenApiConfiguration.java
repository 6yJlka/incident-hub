package ru.donskikh.incidenthub.alertgroup.infrastructure;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class CorrelationOpenApiConfiguration {

    @Bean
    public OpenApiCustomizer correlationEvidenceSchemas() {
        return openApi -> {
            var schemas = openApi.getComponents().getSchemas();
            schemas.put("TemporalProximityEvidence", variant("TEMPORAL_PROXIMITY", Map.of(
                    "alertId", identifier(), "distanceToGroup", new StringSchema()
                            .description("Signed ISO-8601 duration: negative before the group interval, zero inside, positive after")
                            .example("PT20S"))));
            schemas.put("EarliestAlertEvidence", variant("EARLIEST_ALERT", Map.of(
                    "alertId", identifier(), "serviceId", identifier())));
            schemas.put("DependencyPathEvidence", variant("DEPENDENCY_PATH", Map.of(
                    "serviceIds", new ArraySchema().items(identifier()).minItems(2))));
            schemas.put("MigratedEvidence", variant("MIGRATED", Map.of()));
            var discriminator = new Discriminator().propertyName("type")
                    .mapping("TEMPORAL_PROXIMITY", reference("TemporalProximityEvidence"))
                    .mapping("EARLIEST_ALERT", reference("EarliestAlertEvidence"))
                    .mapping("DEPENDENCY_PATH", reference("DependencyPathEvidence"))
                    .mapping("MIGRATED", reference("MigratedEvidence"));
            var evidence = new ComposedSchema().discriminator(discriminator);
            for (String name : List.of("TemporalProximityEvidence", "EarliestAlertEvidence",
                    "DependencyPathEvidence", "MigratedEvidence")) {
                evidence.addOneOfItem(new Schema<>().$ref(reference(name)));
            }
            schemas.put("EvidenceResponse", evidence);
        };
    }

    private ObjectSchema variant(String type, Map<String, Schema<?>> properties) {
        var schema = new ObjectSchema();
        schema.addProperty("type", new StringSchema()._enum(List.of(type)));
        schema.addRequiredItem("type");
        properties.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            schema.addProperty(entry.getKey(), entry.getValue());
            schema.addRequiredItem(entry.getKey());
        });
        schema.setAdditionalProperties(false);
        return schema;
    }

    private Schema<?> identifier() {
        return new IntegerSchema().format("int64").minimum(java.math.BigDecimal.ONE);
    }

    private String reference(String name) {
        return "#/components/schemas/" + name;
    }
}

package ru.donskikh.incidenthub.experiment;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ExperimentIsolationTest {
    @Test
    void experimentIsIsolatedAndCoreHasOnlyPureDependencies() {
        var classes = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("ru.donskikh.incidenthub");
        assertThat(classes).anyMatch(type -> type.getPackageName().startsWith("ru.donskikh.incidenthub.experiment"));
        for (JavaClass type : classes) {
            boolean experiment = type.getPackageName().startsWith("ru.donskikh.incidenthub.experiment");
            type.getDirectDependenciesFromSelf().forEach(dependency -> {
                var target = dependency.getTargetClass();
                String name = target.getName();
                if (!experiment) {
                    assertThat(target.getPackageName()).doesNotStartWith("ru.donskikh.incidenthub.experiment");
                } else {
                    assertThat(name).doesNotStartWith("org.springframework.").doesNotStartWith("jakarta.persistence.");
                    assertThat(name).isNotIn("java.util.HashMap", "java.util.HashSet");
                    if (name.startsWith("ru.donskikh.incidenthub.") && !name.startsWith("ru.donskikh.incidenthub.experiment.")) {
                        assertThat(target.isEnum() || name.startsWith("ru.donskikh.incidenthub.alert.correlation.")).as(name).isTrue();
                    }
                    if (type.getPackageName().endsWith(".graph") || type.getPackageName().endsWith(".scenario")) {
                        assertThat(name).doesNotStartWith("tools.jackson.").doesNotStartWith("com.fasterxml.").doesNotStartWith("ru.donskikh.incidenthub.experiment.io.");
                    }
                }
            });
            if (experiment) type.getMethodCallsFromSelf().forEach(call -> {
                String name = call.getTargetOwner().getName() + "." + call.getName();
                assertThat(name).isNotIn("java.lang.Math.random", "java.lang.System.currentTimeMillis", "java.lang.System.nanoTime", "java.time.Instant.now");
            });
        }
    }
}

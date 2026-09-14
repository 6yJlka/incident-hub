package ru.donskikh.incidenthub.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.donskikh.incidenthub.auth.web.AuthController;
import ru.donskikh.incidenthub.incident.web.IncidentController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerMutationAuthorizationTest {

    private static final String BASE_PACKAGE = "ru.donskikh.incidenthub";
    private static final Set<RequestMethod> MUTATING_METHODS = EnumSet.of(
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.DELETE
    );
    private static final Set<String> EXEMPTIONS = Set.of(
            endpoint(AuthController.class, "register"),
            endpoint(AuthController.class, "login"),
            endpoint(IncidentController.class, "create")
    );

    @Test
    void everyMutatingControllerMethodIsRoleProtectedOrExplicitlyExempt() throws Exception {
        List<String> missingAuthorization = new ArrayList<>();
        Set<String> observedExemptions = new HashSet<>();

        for (Class<?> controller : controllerClasses()) {
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null || Set.of(mapping.method()).stream().noneMatch(MUTATING_METHODS::contains)) {
                    continue;
                }

                String endpoint = endpoint(controller, method.getName());
                if (AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)) {
                    continue;
                }
                if (EXEMPTIONS.contains(endpoint)) {
                    observedExemptions.add(endpoint);
                    continue;
                }
                missingAuthorization.add(endpoint);
            }
        }

        assertThat(missingAuthorization)
                .as("mutating controller methods without @PreAuthorize or an explicit exemption")
                .isEmpty();
        assertThat(observedExemptions)
                .as("explicit exemptions must refer to current mutating controller methods")
                .containsExactlyInAnyOrderElementsOf(EXEMPTIONS);
    }

    private static Set<Class<?>> controllerClasses() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<Class<?>> controllers = new HashSet<>();
        for (var candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
            controllers.add(ClassUtils.forName(
                    candidate.getBeanClassName(),
                    ControllerMutationAuthorizationTest.class.getClassLoader()
            ));
        }
        return controllers;
    }

    private static String endpoint(Class<?> controller, String methodName) {
        return controller.getName() + "#" + methodName;
    }
}

package com.mentesvivas.backend.entity.audit;

import com.mentesvivas.backend.repository.audit.AuditEventRepository;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuditImmutabilityTest {

    @Test
    void entityIsMarkedImmutableAndHasNoSetters() {
        assertThat(AuditEvent.class.isAnnotationPresent(Immutable.class)).isTrue();
        assertThat(Arrays.stream(AuditEvent.class.getDeclaredMethods()).map(Method::getName))
                .noneMatch(name -> name.startsWith("set"));
    }

    @Test
    void repositoryExposesNoUpdateOrDeleteOperations() {
        assertThat(Arrays.stream(AuditEventRepository.class.getMethods()).map(Method::getName))
                .noneMatch(name -> name.startsWith("delete") || name.startsWith("remove") || name.startsWith("update"));
    }

    @Test
    void eventStoresOnlyIdsTimestampsEnumsAndValuesNeverADirectIdentification() {
        Set<Class<?>> allowed = Set.of(UUID.class, Instant.class, AuditEventType.class, String.class);
        for (Field field : AuditEvent.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            assertThat(allowed).as(field.getName()).contains(field.getType());
        }
        assertThat(Arrays.stream(AuditEvent.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("email", "cpf", "fullName", "name", "phone");
    }
}

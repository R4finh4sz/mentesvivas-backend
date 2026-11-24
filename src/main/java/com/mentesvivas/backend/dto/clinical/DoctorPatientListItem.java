package com.mentesvivas.backend.dto.clinical;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DoctorPatientListItem(
        UUID id, String name, LocalDate birthDate, Instant lastActivityAt, boolean pendingAlert
) {}

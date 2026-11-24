package com.mentesvivas.backend.dto.clinical;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DiagnosisResponse(UUID id, String disease, String primaryCid, List<String> secondaryCids, UUID recordedBy,
                                Instant recordedAt) {}

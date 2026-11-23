package com.mentesvivas.backend.service.clinical.timeline;

import java.time.Instant;

public record TimelinePeriod(Instant from, Instant to) {
}

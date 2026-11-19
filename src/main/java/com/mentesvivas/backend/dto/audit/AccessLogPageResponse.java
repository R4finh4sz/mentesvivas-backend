package com.mentesvivas.backend.dto.audit;

import java.util.List;

public record AccessLogPageResponse(
        List<AccessEntryResponse> items,
        int page,
        int size,
        long totalItems
) {
}

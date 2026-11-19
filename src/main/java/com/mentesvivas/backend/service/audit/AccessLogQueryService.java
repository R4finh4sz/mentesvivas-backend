package com.mentesvivas.backend.service.audit;

import com.mentesvivas.backend.dto.audit.AccessEntryResponse;
import com.mentesvivas.backend.dto.audit.AccessLogPageResponse;
import com.mentesvivas.backend.entity.audit.AccessEvent;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.audit.AccessEventRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccessLogQueryService {

    public static final int MAX_PAGE_SIZE = 100;

    private final AccessEventRepository repository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AccessLogPageResponse forPatient(UUID patientId, Instant from, Instant to, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<AccessEvent> result = repository.findByPatientUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(
                patientId, from == null ? Instant.EPOCH : from, to == null ? Instant.MAX.minusSeconds(1) : to,
                PageRequest.of(Math.max(page, 0), safeSize));
        Map<UUID, User> accessors = userRepository.findAllById(
                        result.getContent().stream().map(AccessEvent::getAccessorUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<AccessEntryResponse> items = result.getContent().stream()
                .map(event -> toResponse(event, accessors.get(event.getAccessorUserId())))
                .toList();
        return new AccessLogPageResponse(items, result.getNumber(), safeSize, result.getTotalElements());
    }

    private AccessEntryResponse toResponse(AccessEvent event, User accessor) {
        if (accessor == null) {
            return new AccessEntryResponse(event.getOccurredAt(), event.getArea(), event.getAccessorUserId(),
                    null, null, null, null);
        }
        return new AccessEntryResponse(event.getOccurredAt(), event.getArea(), accessor.getId(),
                accessor.getFullName(), accessor.getRole(), accessor.getCrm(), accessor.getCrmState());
    }
}

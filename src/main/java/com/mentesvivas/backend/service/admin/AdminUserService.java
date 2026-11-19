package com.mentesvivas.backend.service.admin;

import com.mentesvivas.backend.dto.admin.AdminUserPageResponse;
import com.mentesvivas.backend.dto.admin.AdminUserResponse;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.auth.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    public static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final RefreshTokenService refreshTokenService;
    private final AuditTrail auditTrail;

    @Transactional
    public AdminUserPageResponse list(Role role, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("createdAt").ascending());
        Page<User> result = role == null ? userRepository.findAll(pageable) : userRepository.findByRole(role, pageable);
        auditTrail.record(AuditRecord.of(AuditEventType.ADMIN_USERS_LISTED, currentUser.get().getId())
                .withValues(null, role == null ? "ALL" : role.name()));
        return new AdminUserPageResponse(result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), pageable.getPageSize(), result.getTotalElements());
    }

    @Transactional
    public AdminUserResponse deactivate(UUID userId, String justification) {
        User admin = currentUser.get();
        User target = find(userId);
        if (target.getRole() == Role.ADMINISTRATOR || target.getId().equals(admin.getId())) {
            throw new ConflictException("A conta do administrador nao pode ser inativada.");
        }
        if (!target.isActive()) {
            throw new ConflictException("Esta conta ja esta inativa.");
        }
        target.setActive(false);
        refreshTokenService.revokeAll(target);
        auditTrail.record(change(AuditEventType.USER_DEACTIVATED, admin, target, justification, "ACTIVE", "INACTIVE"));
        return toResponse(target);
    }

    @Transactional
    public AdminUserResponse reactivate(UUID userId, String justification) {
        User admin = currentUser.get();
        User target = find(userId);
        if (target.isActive()) {
            throw new ConflictException("Esta conta ja esta ativa.");
        }
        target.setActive(true);
        auditTrail.record(change(AuditEventType.USER_REACTIVATED, admin, target, justification, "INACTIVE", "ACTIVE"));
        return toResponse(target);
    }

    private AuditRecord change(AuditEventType type, User admin, User target, String justification,
                               String before, String after) {
        return new AuditRecord(type, admin.getId(), target.getRole() == Role.PATIENT ? target.getId() : null, null, null,
                justification, before, after + " target=" + target.getId());
    }

    private User find(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.isActive(), user.isEmailConfirmed(),
                user.getRole() == Role.DOCTOR ? user.isCrmVerified() : null, user.getCreatedAt());
    }
}

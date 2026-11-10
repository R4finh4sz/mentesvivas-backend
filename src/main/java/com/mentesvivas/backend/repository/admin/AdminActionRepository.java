package com.mentesvivas.backend.repository.admin;

import com.mentesvivas.backend.entity.admin.AdminAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminActionRepository extends JpaRepository<AdminAction, UUID> {

    List<AdminAction> findByTargetUserIdOrderByPerformedAtAsc(UUID targetUserId);
}

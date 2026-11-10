package com.mentesvivas.backend.repository.user;

import com.mentesvivas.backend.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mentesvivas.backend.entity.user.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    List<User> findByRoleAndCrmVerifiedFalseOrderByCreatedAtAsc(Role role);
}

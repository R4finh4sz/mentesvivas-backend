package com.mentesvivas.backend.repository.user;

import com.mentesvivas.backend.entity.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mentesvivas.backend.entity.user.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    List<User> findByRoleAndCrmVerifiedFalseOrderByCreatedAtAsc(Role role);

    @Query("select u from User u where u.role = com.mentesvivas.backend.entity.user.Role.DOCTOR and u.crmVerified = true "
            + "and (lower(u.fullName) like lower(concat('%', :term, '%')) or u.crm = :term) order by u.fullName")
    List<User> searchVerifiedDoctors(@Param("term") String term, Pageable pageable);
}

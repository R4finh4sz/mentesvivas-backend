package com.mentesvivas.backend.repository.address;

import com.mentesvivas.backend.entity.address.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    Optional<Address> findByUserId(UUID userId);
}

package com.mentesvivas.backend.repository.patient;

import com.mentesvivas.backend.entity.patient.Cid10Entry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Cid10Repository extends JpaRepository<Cid10Entry, String> {

    List<Cid10Entry> findByCodeStartingWithOrderByCodeAsc(String prefix, Pageable limit);
}

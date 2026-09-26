package com.toollix.nfc.repo;

import com.toollix.nfc.model.NfcAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NfcAssignmentRepository extends JpaRepository<NfcAssignment, Long> {
    List<NfcAssignment> findByProfileIdOrderByAssignedAtDesc(Long profileId);
}

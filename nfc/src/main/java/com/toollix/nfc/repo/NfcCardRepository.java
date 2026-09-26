package com.toollix.nfc.repo;

import com.toollix.nfc.model.NfcCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NfcCardRepository extends JpaRepository<NfcCard, Long> {
    Optional<NfcCard> findByInternalCode(String internalCode);
}

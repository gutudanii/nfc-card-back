package com.toollix.nfc.service;

import com.toollix.common.audit.AuditService;
import com.toollix.nfc.model.NfcAssignment;
import com.toollix.nfc.model.NfcCard;
import com.toollix.nfc.repo.NfcAssignmentRepository;
import com.toollix.nfc.repo.NfcCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Manages NFC card lifecycle and assignment states.
 */
@Service
public class NfcService {

    private static final Logger log = LoggerFactory.getLogger(NfcService.class);

    private final NfcCardRepository cardRepo;
    private final NfcAssignmentRepository assignRepo;
    private final AuditService auditService;

    public NfcService(NfcCardRepository cardRepo, NfcAssignmentRepository assignRepo, AuditService auditService) {
        this.cardRepo = cardRepo;
        this.assignRepo = assignRepo;
        this.auditService = auditService;
    }

    @Transactional
    public NfcCard createCard(String internalCode) {
        log.info("[NFC_SVC] createCard — internalCode='{}'", internalCode);
        NfcCard c = new NfcCard();
        c.setInternalCode(internalCode);
        c.setStatus("UNASSIGNED");
        var saved = cardRepo.save(c);
        log.info("[NFC_SVC] createCard — created id={}", saved.getId());

        try {
            auditService.record("nfc.create", "system", "code=" + internalCode);
        } catch (Exception e) {
            log.warn("[AUDIT] nfc.create failed for code={}: {}", internalCode, e.getMessage());
        }
        return saved;
    }

    public Optional<NfcCard> findByInternalCode(String code) {
        log.debug("[NFC_SVC] findByInternalCode — code='{}'", code);
        return cardRepo.findByInternalCode(code);
    }

    @Transactional
    public NfcAssignment assignToProfile(NfcCard card, Long profileId) {
        log.info("[NFC_SVC] assignToProfile — cardId={} profileId={}", card.getId(), profileId);

        var asg = new NfcAssignment();
        asg.setNfcCard(card);
        asg.setProfileId(profileId);
        var saved = assignRepo.save(asg);

        card.setStatus("ASSIGNED");
        cardRepo.save(card);
        log.info("[NFC_SVC] assignToProfile — cardId={} status updated to ASSIGNED", card.getId());

        try {
            auditService.record("nfc.assign", "system", "card=" + card.getInternalCode() + " profileId=" + profileId);
        } catch (Exception e) {
            log.warn("[AUDIT] nfc.assign failed for cardId={}: {}", card.getId(), e.getMessage());
        }
        return saved;
    }

    @Transactional
    public NfcCard markLost(Long cardId) {
        log.info("[NFC_SVC] markLost — cardId={}", cardId);
        var card = cardRepo.findById(cardId).orElseThrow(() -> {
            log.warn("[NFC_SVC] markLost — card not found cardId={}", cardId);
            return new IllegalArgumentException("card_not_found");
        });
        card.setStatus("LOST");
        var saved = cardRepo.save(card);
        log.info("[NFC_SVC] markLost — cardId={} status updated to LOST", saved.getId());
        return saved;
    }

    @Transactional
    public NfcCard retire(Long cardId) {
        log.info("[NFC_SVC] retire — cardId={}", cardId);
        var card = cardRepo.findById(cardId).orElseThrow(() -> {
            log.warn("[NFC_SVC] retire — card not found cardId={}", cardId);
            return new IllegalArgumentException("card_not_found");
        });
        card.setStatus("RETIRED");
        var saved = cardRepo.save(card);
        log.info("[NFC_SVC] retire — cardId={} status updated to RETIRED", saved.getId());
        return saved;
    }

    // Stub implementation to be hooked to an async analytics writer
    public void recordTapAsync(NfcCard card) {
        log.debug("[NFC_SVC] recordTapAsync — cardId={}. Analytics call deferred.", card.getId());
    }
}

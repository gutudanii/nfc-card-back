package com.toollix.nfc.service;

import com.toollix.nfc.model.NfcAssignment;
import com.toollix.nfc.model.NfcCard;
import com.toollix.nfc.repo.NfcAssignmentRepository;
import com.toollix.nfc.repo.NfcCardRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NfcService {

    private final NfcCardRepository cardRepo;
    private final NfcAssignmentRepository assignRepo;
    private final com.toollix.common.audit.AuditService auditService;

    public NfcService(NfcCardRepository cardRepo, NfcAssignmentRepository assignRepo, com.toollix.common.audit.AuditService auditService) {
        this.cardRepo = cardRepo;
        this.assignRepo = assignRepo;
        this.auditService = auditService;
    }

    public NfcCard createCard(String internalCode) {
        NfcCard c = new NfcCard();
        c.setInternalCode(internalCode);
        c.setStatus("UNASSIGNED");
        var saved = cardRepo.save(c);
        try { auditService.record("nfc.create", "system", "code="+internalCode); } catch (Exception ignored) {}
        return saved;
    }

    public Optional<NfcCard> findByInternalCode(String code) { return cardRepo.findByInternalCode(code); }

    public NfcAssignment assignToProfile(NfcCard card, Long profileId) {
        var asg = new NfcAssignment();
        asg.setNfcCard(card);
        asg.setProfileId(profileId);
        card.setStatus("ASSIGNED");
        cardRepo.save(card);
        var saved = assignRepo.save(asg);
        try { auditService.record("nfc.assign", "system", "card="+card.getInternalCode()+" profileId="+profileId); } catch (Exception ignored) {}
        return saved;
    }

    public NfcCard markLost(Long cardId) {
        var card = cardRepo.findById(cardId).orElseThrow(() -> new IllegalArgumentException("card_not_found"));
        card.setStatus("LOST");
        return cardRepo.save(card);
    }

    public NfcCard retire(Long cardId) {
        var card = cardRepo.findById(cardId).orElseThrow(() -> new IllegalArgumentException("card_not_found"));
        card.setStatus("RETIRED");
        return cardRepo.save(card);
    }
}

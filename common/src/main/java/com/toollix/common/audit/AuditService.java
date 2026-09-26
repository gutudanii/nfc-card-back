package com.toollix.common.audit;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Service
public class AuditService {

    private final EntityManager em;

    public AuditService(EntityManager em) {
        this.em = em;
    }

    @Transactional
    public void record(String action, String by, String details) {
        AuditLog a = new AuditLog(action, by, details);
        em.persist(a);
    }
}
